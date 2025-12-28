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
  AWS Access Key and Secret Key are stored using `EncryptedSharedPreferences`.

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
- **UI:** Jetpack Compose (Onboarding), ViewBinding (Dashboard)  
- **Architecture:** MVVM  
- **AWS SDK:** AWS SDK for Kotlin (SNS, STS, EC2)  
- **Push Notifications:** Firebase Cloud Messaging (FCM)  
- **Database:** Room  
- **Security:** EncryptedSharedPreferences  

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


🚧 Status

✅ First public release

📈 Actively improving

🧩 More AWS integrations planned
**
