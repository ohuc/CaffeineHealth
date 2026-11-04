# Caffeine Health privacy policy

**Effective date:** 2 November 2026
**Developer (data controller / data fiduciary):** Uditt Chauhan, publishing as "Health.", India
**Contact and grievances:** healthdot.support@gmail.com

Caffeine Health is a caffeine tracker for Android. It is built to work without an account and
without sending your health information anywhere. This policy explains what the app stores, the
few cases where data leaves your device, and the choices you have.

It covers both editions:

- **The open-source edition** (GitHub, F-Droid, IzzyOnDroid), which has no purchases.
- **The Google Play edition**, which offers the optional Caffeine Health+ purchase. The extra
  data handling for purchases is described under "Purchases" below.

## The short version

- No account and no Caffeine Health server. Your drink history and health profile stay on your
  device. Signing in with Google is only used for the optional Google Drive backup.
- No advertising, no advertising ID, no analytics or tracking SDKs, and no selling of data.
- Data leaves your device only when you choose a feature that needs it: a barcode lookup, Health
  Connect, a backup you make, Google Drive backup (Google Play edition), Android's own device
  backup, or a Plus purchase on Google Play.

## Information stored on your device

The app stores the following **only on your device**, in its private app storage:

- **Drink history:** drinks you log, amounts, caffeine content, times and durations.
- **Custom drinks:** names, caffeine values and any photo you choose for them.
- **Caffeine and sleep profile (optional answers):** age range, body weight, insomnia, smoking,
  alcohol and caffeine habits, liver condition, medications that affect caffeine, CYP1A2/AHR
  genotype, and hormonal status, including pregnancy trimester or hormonal contraceptives.
  These answers are only used on your device to estimate how quickly your body clears caffeine.
- **Sleep settings:** bedtime, weekly sleep schedule and sleep threshold.
- **App settings:** reminders, appearance, language, date and time preferences and similar.

The developer cannot see, access or recover this information. Every profile question is optional.

## When data leaves your device

### Barcode lookup (Open Food Facts)

When you scan a product barcode, the camera image is decoded **on your device** and is never stored
or sent. If you agree to look the product up, the app sends **only the barcode number** to
[Open Food Facts](https://world.openfoodfacts.org), a free, open product database. Like any web
request, this exposes your IP address and an app identifier (the app name and version) to Open
Food Facts. No drink history or profile data is sent. Open Food Facts handles this request under its
own [privacy policy](https://world.openfoodfacts.org/privacy).

### Health Connect (optional)

If you turn it on, Caffeine Health uses Android Health Connect to:

- **write** the caffeine (nutrition) and hydration of drinks you log;
- **read** nutrition records, so caffeine logged by other apps can appear in your history;
- **read** sleep sessions, to adjust your bedtime estimate.

You grant and revoke each permission in Android's Health Connect settings. Data read from Health
Connect is used only inside the app for these features, stays on your device, and is never sent to
the developer or any third party, never used for advertising, and never sold.

The use of information received from Health Connect adheres to the
[Health Connect Permissions policy](https://support.google.com/googleplay/android-developer/answer/12991134),
including the Limited Use requirements.

### Backups you create

**My data → Export** writes a backup file (drink history, custom drinks, profile and settings) to a
location **you** choose. What happens to that file afterwards, for example if you save it to a
cloud drive, is up to you and the service you pick.

### Google Drive backup (Google Play edition, optional)

If you turn on **My data → Google Drive backup**, or choose **Restore from Google Drive** during
setup, you sign in with Google and allow the app to use a private app-data folder in **your own**
Google Drive. The app can see only the files it creates there, not the rest of your Drive.

- **What is stored:** the same backup file as a manual export: drink history, custom drinks, your
  caffeine and sleep profile answers, and settings. It is uploaded once a day while backup is on,
  and when you tap **Back up now**. Only the newest copy is kept.
- **Who can access it:** you, through the app. The file sits in your Google account under
  [Google's Privacy Policy](https://policies.google.com/privacy). The developer has no copy, no
  server and no access to it.
- **Control:** turn backup off at any time; **Delete backup from Google Drive** removes the copy.
  You can also delete it in Google Drive (Settings → Manage apps → Caffeine Health → Delete hidden
  app data) and revoke access in your Google Account's third-party connections.

Caffeine Health's use and transfer of information received from Google APIs adheres to the
[Google API Services User Data Policy](https://developers.google.com/terms/api-services-user-data-policy),
including the Limited Use requirements. That information is used only to store and restore your
own backup, is never used for advertising, and is never sold or shared.

### Android device backup

If Android backup is enabled on your phone, Android may include the app's **settings file** in your
Google account backup and in device-to-device transfers. That file contains your preferences **and
your caffeine and sleep profile answers**. Your drink history and purchase state are excluded.
Google handles this backup under its own terms. You can turn it off in your phone's backup settings.

### Fonts

The app may request its typeface (Montserrat) through the font service in Google Play services on
your device. No personal data is included in that request.

### Automation and widgets

Automation lets apps you set up (for example Tasker or MacroDroid) log drinks by sending a command
to Caffeine Health. Home-screen widgets show your current caffeine level. Both work locally and
send nothing to the developer.

### Notifications

Reminders are scheduled and shown on your device. No notification service or server is involved.

## Purchases (Google Play edition only)

Caffeine Health+ is an optional purchase. Everything in the free app keeps working without it.

- **Google Play** processes the payment. Google receives your payment details and Google account
  under the [Google Privacy Policy](https://policies.google.com/privacy). The developer never
  sees your card or payment details.
- **RevenueCat** (RevenueCat, Inc., United States) verifies purchases on the developer's behalf.
  To do this it receives a **random, anonymous app user ID** created by the app (not your name,
  email or Google account), your purchase records from Google Play (product, purchase and renewal
  dates, transaction identifiers, price and currency, country), the app version, basic device and
  operating-system information, and your IP address. Collection of device identifiers such as
  the advertising ID is turned off. RevenueCat handles this data under its
  [privacy policy](https://www.revenuecat.com/privacy).
- **No health data is shared for purchases.** Your drink history, health profile and sleep data
  are never sent to Google Play Billing or RevenueCat.
- The app keeps a small local record of your purchase status and your one free trial day. It is
  stored privately, is not included in backups, and is removed when you uninstall the app.
- **Restore purchases** restores access only. It does not restore drink history, which is never
  stored anywhere outside your device and your own backups.

Purchase data is processed to provide and verify what you bought. RevenueCat and Google may store
it outside your country, including in the United States.

## Sharing and disclosure

The developer does not sell, rent or trade personal data, and does not share it for advertising.
Apart from the services named above, which you use by choosing the related feature, data is only
disclosed when required by law or valid legal process, or when necessary to protect the rights,
safety or security of users or others. In practice the developer holds very little: no drink
history, no health profile and no account. Outside your device there are only the anonymous
purchase records described above and, if you turn it on, your own Google Drive backup, which the
developer cannot access.

## Permissions

| Permission | Why |
|---|---|
| Camera | Scanning barcodes, decoded on your device |
| Internet | Barcode lookups (both editions), purchases and Google Drive backup (Google Play edition) |
| Notifications | Reminders you set up |
| Run at startup | Re-scheduling your reminders after the phone restarts |
| Health Connect (nutrition, hydration, sleep) | The optional Health Connect features above |

## Retention and deletion

- Data on your device stays until you delete it. You can delete individual drinks, clear all of
  the app's data in Android (Settings → Apps → Caffeine Health → Storage → Clear storage), or
  uninstall the app; either removes everything the app stored.
- The Google Drive backup can be deleted in the app (**My data → Delete backup from Google
  Drive**) or in Google Drive. Step-by-step instructions: [Delete your data](https://ritzy-apricot-3b1.notion.site/Caffeine-Health-Delete-your-data-40c80f6db08e8088b54ed6362f8887f9).
- Revoke Health Connect access at any time in Health Connect settings. Data the app already wrote
  to Health Connect can be deleted there.
- For Plus purchases, the anonymous purchase records held by RevenueCat can be deleted on request:
  email healthdot.support@gmail.com. Include the app's anonymous ID if you can; otherwise describe the purchase.
  Google keeps its own order records under its policy.

## Your rights

Depending on where you live (for example under the EU/UK GDPR or India's Digital Personal Data
Protection Act, 2023), you may have rights to access, correct, delete or port your personal data,
to withdraw consent, and to complain to a data protection authority. Because the developer holds no
account or health data about you, most of these rights are exercised directly in the app. For
anything else, including purchase records and grievances, contact healthdot.support@gmail.com. The developer
aims to respond within 30 days.

## Children

Caffeine Health is intended for adults aged 18 and over and is not directed at children. The developer does not
knowingly collect personal data from children. If you believe a child has made a purchase or
otherwise provided personal data through the app, contact healthdot.support@gmail.com so it can be removed.

## Security

Data is kept in the app's private storage, which other apps cannot read. Network requests use
HTTPS. Exported backup files are not encrypted, so store them somewhere you trust. No method of
storage or transmission is completely secure, but keeping data on your device by default keeps the
amount at risk small.

## Not medical advice

Caffeine estimates, sleep forecasts and Coach suggestions are informational and are not medical
advice.

## Changes to this policy

If this policy changes, the new version will be published at this address with a new effective
date, and significant changes will be noted in the app's release notes.

## Contact

Uditt Chauhan ("Health.") · healthdot.support@gmail.com
