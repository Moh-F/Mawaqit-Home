package com.moh.mawaqithome;

import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * A tiny ADB client (protocol v1, 4 KB payloads, checksums on) so old adbd versions such as the
 * one on Fire OS 6 accept it. It logs every stage, which makes failures diagnosable.
 */
final class MiniAdb implements AutoCloseable {

    private static final int A_CNXN = 0x4e584e43, A_AUTH = 0x48545541, A_OPEN = 0x4e45504f,
            A_OKAY = 0x59414b4f, A_WRTE = 0x45545257, A_CLSE = 0x45534c43;
    private static final int VERSION = 0x01000000, MAXDATA = 4096;
    private static final byte[] SHA1_PREFIX = {0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02,
            0x1a, 0x05, 0x00, 0x04, 0x14};

    private final Socket sock;
    private final InputStream in;
    private final OutputStream out;

    private static final class Msg {
        int cmd, a0, a1;
        byte[] data = new byte[0];

        String name() {
            return cmd == A_CNXN ? "CNXN" : cmd == A_AUTH ? "AUTH" : cmd == A_OKAY ? "OKAY"
                    : cmd == A_WRTE ? "WRTE" : cmd == A_CLSE ? "CLSE" : cmd == A_OPEN ? "OPEN"
                    : Integer.toHexString(cmd);
        }
    }

    // ------------------------------------------------------------------ keys

    static final class Keys {
        PrivateKey priv;
        byte[] pubAdb;           // adb "RSAPublicKey" text, base64 + name + NUL
    }

    static Keys loadKeys(File dir) throws Exception {
        File pf = new File(dir, "adbkey2"), qf = new File(dir, "adbkey2.pub");
        if (!pf.exists() || !qf.exists()) {
            KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
            g.initialize(2048);
            KeyPair kp = g.generateKeyPair();
            write(pf, kp.getPrivate().getEncoded());
            write(qf, adbPublicKey((RSAPublicKey) kp.getPublic()));
            AppLog.info("ADB", "generated a new key pair");
        }
        Keys k = new Keys();
        k.priv = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(read(pf)));
        k.pubAdb = read(qf);
        return k;
    }

    static void deleteKeys(File dir) {
        new File(dir, "adbkey2").delete();
        new File(dir, "adbkey2.pub").delete();
    }

    private static void write(File f, byte[] b) throws Exception {
        FileOutputStream o = new FileOutputStream(f);
        o.write(b);
        o.close();
    }

    private static byte[] read(File f) throws Exception {
        FileInputStream i = new FileInputStream(f);
        byte[] b = new byte[(int) f.length()];
        new DataInputStream(i).readFully(b);
        i.close();
        return b;
    }

    private static byte[] leBytes(BigInteger v, int len) {
        byte[] be = v.toByteArray();
        byte[] le = new byte[len];
        for (int i = 0; i < len && i < be.length; i++) le[i] = be[be.length - 1 - i];
        return le;
    }

    /** Android's own RSAPublicKey struct (2048-bit): len, n0inv, modulus, rr, exponent. */
    private static byte[] adbPublicKey(RSAPublicKey pub) {
        BigInteger n = pub.getModulus();
        BigInteger r32 = BigInteger.ONE.shiftLeft(32);
        long n0inv = n.mod(r32).modInverse(r32).negate().mod(r32).longValue();
        BigInteger rr = BigInteger.ONE.shiftLeft(2048).modPow(BigInteger.valueOf(2), n);
        ByteBuffer bb = ByteBuffer.allocate(4 + 4 + 256 + 256 + 4).order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(64);
        bb.putInt((int) n0inv);
        bb.put(leBytes(n, 256));
        bb.put(leBytes(rr, 256));
        bb.putInt(pub.getPublicExponent().intValue());
        byte[] b64 = Base64.encode(bb.array(), Base64.NO_WRAP);
        byte[] tail = " mawaqit@home\0".getBytes();
        byte[] all = new byte[b64.length + tail.length];
        System.arraycopy(b64, 0, all, 0, b64.length);
        System.arraycopy(tail, 0, all, b64.length, tail.length);
        return all;
    }

    // -------------------------------------------------------------- protocol

    private MiniAdb(Socket s) throws Exception {
        sock = s;
        in = s.getInputStream();
        out = s.getOutputStream();
    }

    private void send(int cmd, int a0, int a1, byte[] data) throws Exception {
        int sum = 0;
        for (byte b : data) sum += b & 0xff;
        ByteBuffer h = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN);
        h.putInt(cmd).putInt(a0).putInt(a1).putInt(data.length).putInt(sum).putInt(cmd ^ 0xffffffff);
        out.write(h.array());
        if (data.length > 0) out.write(data);
        out.flush();
    }

    private Msg recv() throws Exception {
        byte[] hb = new byte[24];
        new DataInputStream(in).readFully(hb);          // EOFException when the peer hangs up
        ByteBuffer h = ByteBuffer.wrap(hb).order(ByteOrder.LITTLE_ENDIAN);
        Msg m = new Msg();
        m.cmd = h.getInt();
        m.a0 = h.getInt();
        m.a1 = h.getInt();
        int len = h.getInt();
        if (len < 0 || len > 1 << 20) throw new Exception("bad length " + len);
        m.data = new byte[len];
        if (len > 0) new DataInputStream(in).readFully(m.data);
        return m;
    }

    static MiniAdb connect(String host, int port, Keys k) throws Exception {
        Socket s = new Socket();
        s.setTcpNoDelay(true);
        s.connect(new InetSocketAddress(host, port), 4000);
        s.setSoTimeout(40000);                           // time to answer the TV's prompt
        MiniAdb a = new MiniAdb(s);
        try {
            a.send(A_CNXN, VERSION, MAXDATA, "host::\0".getBytes());
            AppLog.info("ADB", host + ": sent CNXN");
            boolean sentSig = false, sentKey = false;
            while (true) {
                Msg m = a.recv();
                AppLog.info("ADB", host + ": got " + m.name() + " a0=" + m.a0 + " len=" + m.data.length);
                if (m.cmd == A_CNXN) {
                    AppLog.info("ADB", host + ": device banner " + new String(m.data).trim());
                    return a;
                }
                if (m.cmd != A_AUTH) throw new Exception("unexpected " + m.name());
                if (!sentSig) {
                    byte[] in2 = new byte[SHA1_PREFIX.length + m.data.length];
                    System.arraycopy(SHA1_PREFIX, 0, in2, 0, SHA1_PREFIX.length);
                    System.arraycopy(m.data, 0, in2, SHA1_PREFIX.length, m.data.length);
                    Signature sg = Signature.getInstance("NONEwithRSA");
                    sg.initSign(k.priv);
                    sg.update(in2);
                    a.send(A_AUTH, 2, 0, sg.sign());
                    sentSig = true;
                    AppLog.info("ADB", host + ": sent signature");
                } else if (!sentKey) {
                    a.send(A_AUTH, 3, 0, k.pubAdb);
                    sentKey = true;
                    AppLog.info("ADB", host + ": sent public key (accept the prompt on the TV)");
                } else {
                    throw new Exception("authorisation was refused");
                }
            }
        } catch (Exception e) {
            a.close();
            throw e;
        }
    }

    /** Runs "shell:cmd" and returns {exitCode, output}. */
    Object[] shell(String cmd) throws Exception {
        send(A_OPEN, 1, 0, ("shell:" + cmd + "; echo __rc=$?\0").getBytes("UTF-8"));
        ByteArrayOutputStream all = new ByteArrayOutputStream();
        int remote = 0;
        while (true) {
            Msg m = recv();
            if (m.cmd == A_OKAY) {
                remote = m.a0;
            } else if (m.cmd == A_WRTE) {
                all.write(m.data);
                send(A_OKAY, 1, m.a0, new byte[0]);
            } else if (m.cmd == A_CLSE) {
                break;
            }
        }
        String txt = all.toString("UTF-8");
        int rc = 1;
        int i = txt.lastIndexOf("__rc=");
        if (i >= 0) {
            try {
                rc = Integer.parseInt(txt.substring(i + 5).trim());
            } catch (Exception ignored) {
            }
            txt = txt.substring(0, i);
        }
        return new Object[]{rc, txt.trim()};
    }

    @Override public void close() {
        try {
            sock.close();
        } catch (Exception ignored) {
        }
    }
}
