# AWS Notifier Android App

An Android application for managing AWS Simple Notification Service (SNS) topics and subscriptions. This app allows you to connect to your AWS account, manage topics, and subscribe your device to receive push notifications.

## Features

-   **AWS Integration**: Securely connect to your AWS account using IAM credentials.
-   **Multi-Region Support**: Switch between different AWS regions seamlessly.
-   **Topic Management**:
    -   List all available SNS topics in a selected region.
    -   Create new SNS topics.
    -   Delete existing topics directly from the app.
-   **Subscription Management**:
    -   Subscribe your device to any topic to receive push notifications.
    -   Unsubscribe from topics to stop receiving notifications.
-   **Push Notifications**: Leverages Firebase Cloud Messaging (FCM) to receive notifications from subscribed topics.
-   **Local Persistence**: Remembers your credentials, region, and subscription state.
-   **Search**: Easily find topics by name.

## How It Works

1.  **Onboarding**: The first time you launch the app, you'll go through an onboarding process where you need to provide your AWS IAM Access Key and Secret Key. These keys must belong to a user with sufficient permissions to manage SNS.
2.  **Region & Platform ARN**: Select the AWS region you want to work in. The app requires a Platform Application ARN (created in AWS SNS for your Firebase project) to register for push notifications. You will be prompted to enter this ARN for the selected region.
3.  **Device Registration**: The app automatically retrieves an FCM token and registers itself with AWS SNS. This creates a platform endpoint for your device.
4.  **Topic Management**: Once set up, the main screen will display a list of your SNS topics in the selected region. You can refresh this list, create new topics, or delete them.
5.  **Subscribing**: Simply tap the "Subscribe" button on a topic to start receiving notifications. You can unsubscribe at any time.

## Screenshots

** Notification Screenshot **
![1000085126](https://github.com/user-attachments/assets/ca32972c-9644-4325-a3b3-fd65338c966c)

| Onboarding Screen                               | Main Screen (Topic List)                          | Create Topic Dialog                             |
| ----------------------------------------------- | ------------------------------------------------- | ----------------------------------------------- |
| ![Onboarding](<path_to_onboarding_screenshot.png>) | ![Main Screen](<path_to_main_screen_screenshot.png>) | ![Create Topic](<path_to_dialog_screenshot.png>) |

## Setup & Configuration

### Prerequisites

-   An AWS account.
-   A Firebase project linked to the Android app.

### Configuration Steps

1.  **IAM User**:
    Create an IAM user in your AWS account with programmatic access. This user needs permissions to perform SNS actions. It is recommended to create a specific policy with the following minimum permissions:
    -   `sns:ListTopics`
    -   `sns:CreateTopic`
    -   `sns:DeleteTopic`
    -   `sns:Subscribe`
    -   `sns:Unsubscribe`
    -   `sns:CreatePlatformEndpoint`
    -   `sns:GetEndpointAttributes`
    -   `sns:SetEndpointAttributes`

    **Note:** For security, grant only the necessary permissions. Do not use your root account credentials.

2.  **Firebase Setup**:
    -   Set up a Firebase project at the [Firebase Console](https://console.firebase.google.com/).
    -   Add your Android app to the project and download the `google-services.json` file. Place this file in the `app/` directory of the project.

3.  **AWS SNS Platform Application**:
    -   In the AWS SNS console, create a "Platform Application".
    -   Choose "Firebase Cloud Messaging (FCM)" and enter your FCM Server Key from the Firebase project settings.
    -   Once created, copy the **Platform Application ARN**. The app will ask for this ARN during the setup process for a specific region.

4.  **Build the App**:
    -   Open the project in Android Studio.
    -   The app stores credentials securely. The specific implementation details regarding environment variables and credential storage are managed within the app's secure configuration. When prompted by the app, enter your IAM Access Key and Secret Key.

### `local.properties`

For local development, you can store your AWS credentials in the `local.properties` file at the root of the project. This file is included in `.gitignore` and will not be checked into version control.

Create a `local.properties` file if it doesn't exist and add the following lines:

```
aws.accessKeyId=YOUR_AWS_ACCESS_KEY_ID
aws.secretKey=YOUR_AWS_SECRET_KEY
```

Replace `YOUR_AWS_ACCESS_KEY_ID` and `YOUR_AWS_SECRET_KEY` with your actual IAM user credentials.

If these properties are not found in `local.properties`, the app will prompt you to enter them manually.
