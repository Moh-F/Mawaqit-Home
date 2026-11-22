package com.moh.mawaqithome;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/** Tiny HTTP server on the local network: a phone/laptop opens a page, types text or downloads a file. */
final class PhoneServer {
    static final class Resp {
        final int code;
        final String type;
        final byte[] data;

        Resp(int code, String type, byte[] data) {
            this.code = code;
            this.type = type;
            this.data = data;
        }

        static Resp html(String body) {
            try {
                return new Resp(200, "text/html; charset=utf-8", ("<!doctype html><html dir=rtl><meta charset=utf-8>"
                        + "<meta name=viewport content='width=device-width,initial-scale=1'>"
                        + "<body style='font-family:sans-serif;margin:14px'>" + body).getBytes("UTF-8"));
            } catch (Exception e) {
                return new Resp(500, "text/plain", new byte[0]);
            }
        }
    }

    interface Handler {
        /** body = request body (POST) or empty. */
        Resp handle(String method, String path, byte[] body);
    }

    private ServerSocket ss;

    boolean start(final int port, final Handler h) {
        stop();
        try {
            final ServerSocket s = new ServerSocket(port);
            ss = s;
            new Thread(() -> {
                while (!s.isClosed()) {
                    try {
                        Socket c = s.accept();
                        try {
                            c.setSoTimeout(8000);
                            InputStream in = c.getInputStream();
                            ByteArrayOutputStream hd = new ByteArrayOutputStream();
                            int b, state = 0;
                            while (hd.size() < 16384 && (b = in.read()) >= 0) {
                                hd.write(b);
                                state = (b == '\r' && (state == 0 || state == 2)) ? state + 1
                                        : (b == '\n' && (state == 1 || state == 3)) ? state + 1 : 0;
                                if (state == 4) break;
                            }
                            String head = hd.toString("UTF-8");
                            String[] first = head.split("\r\n")[0].split(" ");
                            String method = first.length > 0 ? first[0] : "GET";
                            String path = first.length > 1 ? first[1] : "/";
                            int len = 0;
                            for (String line : head.split("\r\n"))
                                if (line.toLowerCase().startsWith("content-length:"))
                                    len = Integer.parseInt(line.substring(15).trim());
                            byte[] body = new byte[Math.min(len, 2_000_000)];
                            int got = 0;
                            while (got < body.length) {
                                int n = in.read(body, got, body.length - got);
                                if (n < 0) break;
                                got += n;
                            }
                            Resp r = h.handle(method, path, body);
                            OutputStream o = c.getOutputStream();
                            o.write(("HTTP/1.1 " + r.code + (r.code == 200 ? " OK" : " Error")
                                    + "\r\nContent-Type: " + r.type + "\r\nContent-Length: " + r.data.length
                                    + "\r\nConnection: close\r\n\r\n").getBytes("UTF-8"));
                            o.write(r.data);
                            o.flush();
                        } finally {
                            c.close();
                        }
                    } catch (Exception e) {
                        if (s.isClosed()) break;
                    }
                }
            }, "phonesrv").start();
            return true;
        } catch (Exception e) {
            AppLog.error("PHONESRV", "start failed", e);
            return false;
        }
    }

    void stop() {
        try {
            if (ss != null) ss.close();
        } catch (Exception ignored) {
        }
        ss = null;
    }
}
