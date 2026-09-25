# 🚀 AWS Notifier — Android App

**AWS Notifier** is an Android application designed for **developers and DevOps engineers** to manage **AWS SNS topics** and receive **real-time system or cost alerts** directly on their mobile devices.

It provides a secure, mobile-first way to manage SNS topics, subscriptions, and notifications across multiple AWS regions.

---

## ✨ Key Features

- **Multi-Region Support**  
  Manage SNS topics across all major AWS regions (US, EU, AP, SA).

- **Topic Management**  
  Create, delete, refresh, and list SNS topics directly from your phone.

- **Subscription Control**  
  One-tap subscribe or unsubscribe your device to specific SNS topics.

- **Real-time Push Notifications**  
  Instant alerts delivered via **Firebase Cloud Messaging (FCM)**.

- **Secure Credential Storage**  
  AWS Access Key and Secret Key are encrypted at rest with a hardware-backed Android Keystore key (AES-GCM).

- **Notification History**  
  Local storage of received alerts with automatic retention and cleanup.

- **Search & Filtering**  
  Quickly find SNS topics by name.

---

## 📱 Screenshots

### 🚀 Onboarding & AWS Setup
Secure onboarding flow with IAM best practices and guided AWS credential setup.

<p float="left">
  <img src="https://github.com/user-attachments/assets/058cd774-4650-4297-8834-b50e0645609f" width="250"/>
  <img src="https://github.com/user-attachments/assets/aae275cf-297e-4f69-a0c5-142b6976e8f2" width="250"/>
  <img src="https://github.com/user-attachments/assets/e09036d9-f952-4c31-b577-1b89b374da3f" width="250"/>
  <img src="https://github.com/user-attachments/assets/f3ff96d5-7cf3-4065-9666-2a36da11a5c7" width="250"/>
  <img src="https://github.com/user-attachments/assets/1570c81b-6093-4c4b-b4d4-80bda95be43d" width="250"/>
  <img src="https://github.com/user-attachments/assets/de78a7d3-3a70-4e08-8efc-00425e1c9ca7" width="250"/>
</p>

---

### 📊 Dashboard & SNS Topics
Manage SNS topics across regions with real-time subscription control.

<p float="left">
  <img src="https://github.com/user-attachments/assets/023e36af-9a86-4772-af9f-01bcc917be1a" width="250"/>
  <img src="https://github.com/user-attachments/assets/5ef3f234-7eb1-468f-9cba-64021d4c5070" width="250"/>
</p>

---

### 🔔 Notification History
View received alerts with automatic cleanup based on retention policy.

<p float="left">
  <img src="https://github.com/user-attachments/assets/ed5c6560-7e80-4f05-a38f-8e36ddf79fc1" width="250"/>
</p>

---

### ⚙️ Settings & Security
Manage AWS account details, notification retention, and securely reset credentials.

<p float="left">
  <img src="https://github.com/user-attachments/assets/8708d9ae-3fa0-46f5-aa1a-398d57dbb9f0" width="250"/>
</p>

---

### 🆕 v1.2.2 — Real Billing Alerts, Fully Compose UI
Confirmed end-to-end: a real AWS Budgets or CloudWatch alarm publishing to SNS now renders
its actual name and reason/amount in the notification, instead of falling back to generic text.
The app is also now fully migrated to Jetpack Compose — no XML layouts remain.

<p float="left">
  <img src="docs/screenshots/topics-list.png" width="250"/>
  <img src="docs/screenshots/region-filtered-topics.png" width="250"/>
  <img src="docs/screenshots/billing-alarm-notification.png" width="250"/>
</p>

---

## 🧠 How It Works

1. **Onboarding**  
   Complete a guided onboarding flow and provide AWS IAM credentials with SNS permissions.

2. **Region & Platform Application ARN**  
   Select an AWS region and provide the SNS **Platform Application ARN** associated with your Firebase project.

3. **Device Registration**  
   The app retrieves an FCM token and creates an SNS platform endpoint for your device.

4. **Topic Management**  
   View, create, refresh, or delete SNS topics in the selected region.

5. **Subscriptions**  
   Subscribe to any topic with a single tap to start receiving push notifications instantly.

6. **Notification History**  
   Alerts are stored locally and automatically cleaned up based on retention settings.

---

## 🛠 Tech Stack

- **Language:** Kotlin  
- **UI:** Jetpack Compose (100% — no XML layouts)  
- **Architecture:** MVVM  
- **AWS SDK:** AWS SDK for Kotlin (SNS, STS, EC2)  
- **Push Notifications:** Firebase Cloud Messaging (FCM)  
- **Database:** Room  
- **Security:**  Android Keystore (hardware-backed AES-GCM)

---

## 🔐 Setup & Configuration

### Prerequisites

- An AWS account  
- A Firebase project linked to the Android app  

---

### 1️⃣ IAM User Setup

Create an IAM user with **programmatic access** and attach a policy with the following minimum permissions:

sns:ListTopics
sns:CreateTopic
sns:DeleteTopic
sns:Subscribe
sns:Unsubscribe
sns:CreatePlatformEndpoint
sns:GetEndpointAttributes
sns:SetEndpointAttributes



⚠️ **Do not use root credentials.** Grant only the permissions required.

---

### 2️⃣ Firebase Setup

1. Create a Firebase project  
2. Add your Android app  
3. Download `google-services.json`  
4. Place it in the `app/` directory  

---

### 3️⃣ AWS SNS Platform Application

1. Open the AWS SNS Console  
2. Create a **Platform Application**  
3. Select **Firebase Cloud Messaging (FCM)**  
4. Provide your FCM Server Key  
5. Copy the **Platform Application ARN**  
6. Enter this ARN during onboarding for each region  

---

## 📦 Installation

1. Download `AWS_SNS_NOTIFIER_1.0.0.apk` from **Releases → Assets**  
2. Install the APK on your Android device  
3. Allow installation from unknown sources if prompted  
4. Launch the app and complete onboarding  

---
## Why This App Exists

AWS Budget alerts are critical for monitoring costs, but the default notification options (email, SMS) have limitations:

- **Email:** Slow and easy to miss.  
- **SMS:** Paid service.  
- **SNS:** Free, AWS-native, but not directly mobile-friendly.  

This app fills the gap by bridging **AWS SNS → Android push notifications via FCM**.

---

## Design Decisions

### 1. AWS Credentials in the App
- Users provide their **own AWS Access Key and Secret Key**.  
- No backend or sign-up required, allowing **independent use**.  
- ⚠️ **Security:** Create IAM users with **minimal SNS permissions** for safety.

### 2. Android-only Platform
- Targeting Android ensures **immediate mobile notifications**.  
- Focused on personal use for simplicity and reliability.

### 3. Firebase Cloud Messaging (FCM)
- FCM is used to **deliver push notifications** reliably on Android.  
- Handles token management, registration, and delivery without extra complexity.

### 4. Per-topic Subscriptions
- Users **choose which topics to subscribe to**, avoiding notification overload.  
- Supports multiple use cases: budgets, development, or other alerts.

### 5. Multi-region Support
- AWS SNS topics are **region-scoped**, and the app supports multiple regions.  
- Enables users to monitor alerts across different projects and locations.

### 6. Full SNS Management
- Users can **list, create, delete, and subscribe to topics** directly in the app.  
- Adds convenience and eliminates the need to use AWS Console.

### 7. Independent, Self-contained
- No backend or SMS costs required.  
- Works like a **mobile AWS SNS client**, giving users **full control** over notifications.

---

## Use Case Example

**AWS Budget Alerts:**  
- Set thresholds in AWS Budgets.  
- When a threshold is exceeded, AWS publishes a notification to an SNS topic.  
- This app subscribes to that topic and **delivers a push notification to your Android device instantly**.

---

🚧 Status

✅ First public release

📈 Actively improving

🧩 More AWS integrations planned

## 🤝 Contributing
- Fork the repo
- Create a new branch
- Submit pull requests
**
