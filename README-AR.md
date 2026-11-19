# Mawaqit Home — شاشة رئيسية حيّة لأوقات الصلاة (Fire TV / Google TV / شاشات أندرويد)

تطبيق خلفيته صفحة Mawaqit **حيّة بالكامل** (الساعة بالثواني، الأوقات، العدّ التنازلي)،
وفوقها مربعات تطبيقات شفافة، وساعة وطقس بالزوايا، ووضع حماية للشاشة.

## 1) البناء على ويندوز
1. ثبّت **Android Studio**.
2. File ← Open ← مجلد `MawaqitHome` ← انتظر **Gradle Sync**.
3. Build ← Build APK(s) ← الملف: `app\build\outputs\apk\debug\app-debug.apk`

## 2) التثبيت
```
adb connect <عنوان-الجهاز>:5555
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell monkey -p com.moh.mawaqithome -c android.intent.category.LEANBACK_LAUNCHER 1
```
التحديث بـ `-r` يحتفظ بالمفضلة والإعدادات.

## 3) الاستخدام
- **التبويبات** (سهم لفوق): ★ المفضلة | تطبيقات التلفزيون | تطبيقات الجوال | كل التطبيقات | ⚙ الإعدادات.
- **OK**: يفتح التطبيق. **OK مطوّلة** (أو ضغطة مطوّلة باللمس): قائمة (تحريك، مفضلة، إخفاء، معلومات، إلغاء تثبيت).
- **التحريك:** الأسهم تنقل التطبيق، و**OK/Back** للتثبيت. ترتيب المفضلة مستقل، وباقي التبويبات ترتيبها مشترك.
- **أعلى اليسار** ساعة وتاريخ، **أعلى اليمين** طقس اليوم والغد.

### تبويب ⚙ الإعدادات
إعدادات النظام، الشبكة، الشاشة، الصوت، البلوتوث، إدارة التطبيقات، التخزين، خيارات المطوّر،
وضع حماية الشاشة، **فحص أوقات الصلاة والإيقاظ**، التطبيقات المخفية، نسخ احتياطي، استعادة، التشغيل عند الإقلاع، تحديث الصفحة.

## 4) وضع حماية الشاشة
حسب الوقت بدون أي ضغطة ريموت (الإعداد الافتراضي):
1. 10 د: تختفي الأيقونات والويدجات وتبقى الصفحة وحدها.
2. 15 د: تعتيم.
3. 20 د: شاشة سوداء (الصفحة تعمل خلفها، فيصل صوت الأذان).

- أي زر يوقظ (الضغطة الأولى للإيقاظ فقط).
- **حول كل أذان** (من 5 د قبله إلى 20 د بعده) لا يحصل تعتيم ولا سواد، فقط إخفاء الأيقونات.
- **تحريك بكسل:** كل دقيقة يتحرك كل شيء 3dp تقريباً لحماية OLED من الحرق.

### الإيقاظ قبل الأذان
- أوقات الصلاة تُقرأ من صفحة Mawaqit نفسها (`confData.times`)، وإن لم تتوفر تُجلب من Aladhan (طريقة Diyanet).
- **افحص المصدر:** ⚙ ← «فحص أوقات الصلاة والإيقاظ». إذا ظهر «بديل» يعني أن قراءة الصفحة لم تنجح فأرسل لي ما يظهر.
- قبل كل أذان بـ 5 دقائق: إذا كان الجهاز نائماً (الشاشة مطفأة) يستيقظ ويفتح التطبيق. وإذا كنت تشاهد شيئاً **لا يقاطعك**.

### النوم الحقيقي (تجريبي، اختياري)
يضع الجهاز كله في وضع النوم بعد 30 د بدون استخدام (قد يُطفئ التلفزيون عبر HDMI-CEC إذا كان مفعّلاً في Fire TV: إعدادات ← الشاشة والصوت ← التحكم بأجهزة HDMI CEC).
1. مرة واحدة فقط:
```
adb shell dpm set-active-admin com.moh.mawaqithome/.AdminReceiver
```
2. ⚙ ← وضع حماية الشاشة ← «إطفاء حقيقي (تجريبي)».
- لا ينام الجهاز إلا إذا كانت أوقات الصلاة معروفة (حتى يوقظه المنبّه قبل الأذان).
- بعد الاستيقاظ بالريموت قد تصل إلى الواجهة الأصلية، فافتح Mawaqit Home منها.
- لإلغاء الصلاحية: `adb shell dpm remove-active-admin com.moh.mawaqithome/.AdminReceiver`

## 5) نسخ احتياطي واستعادة
⚙ ← «نسخ احتياطي» يحفظ المفضلة والترتيب والمخفي والإعدادات بملف:
`/sdcard/Android/data/com.moh.mawaqithome/files/mawaqit-home-backup.json`
```
adb pull /sdcard/Android/data/com.moh.mawaqithome/files/mawaqit-home-backup.json
adb push mawaqit-home-backup.json /sdcard/Android/data/com.moh.mawaqithome/files/
```
(ثم ⚙ ← «استعادة» على الجهاز الجديد).

## 6) اختبار اختصارات الإعدادات يدوياً (إن فتح اختصار الإعدادات الرئيسية بدل صفحته)
```
adb shell am start -a android.settings.WIFI_SETTINGS
adb shell am start -a android.settings.DISPLAY_SETTINGS
adb shell am start -a android.settings.SOUND_SETTINGS
adb shell am start -a android.settings.BLUETOOTH_SETTINGS
adb shell am start -a android.settings.MANAGE_ALL_APPLICATIONS_SETTINGS
adb shell am start -a android.settings.INTERNAL_STORAGE_SETTINGS
adb shell am start -a android.settings.APPLICATION_DEVELOPMENT_SETTINGS
```

## 7) تغيير الرابط أو موقع الطقس
```
adb shell am start -n com.moh.mawaqithome/.MainActivity --es url "https://mawaqit.net/ar/xxxx"
```
موقع الطقس وأوقات الصلاة البديلة: الثوابت `LAT` و`LON` أعلى `MainActivity.java`.

## ملاحظات
- يعيد تحميل الصفحة كل 3 ساعات، ويعيد المحاولة كل 20 ثانية عند انقطاع النت.
- زر **Home** في Fire OS يرجّعك دائماً للواجهة الأصلية/Projectivy (من النظام).
- **التشغيل عند الإقلاع:** يعمل على Fire OS 6؛ على أندرويد 10+ (Google TV الحديث) النظام قد يمنعه.
- التطبيق يعمل باللمس أيضاً (شاشات أندرويد): نقرة للفتح وضغطة مطوّلة للقائمة.
