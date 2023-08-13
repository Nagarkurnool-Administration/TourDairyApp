# TourDairy Android App

TourDairy is an Android app that allows users to capture and upload images to a web form. It integrates a WebView to display a web page and provides functionality to choose images from the device's gallery or capture images using the camera for uploading.

## Features

- Display a web page within the app using WebView.
- Enable geolocation permissions for web content.
- Handle file upload requests from web content using the camera or gallery.
- Capture images from the camera and pass them to the web form.
- Choose images from the device's gallery and pass them to the web form.

## Installation

1. Clone this repository to your local machine.
2. Open the project in Android Studio.
3. Build and run the app on an Android device or emulator.

## Usage

1. Upon launching the app, the main activity displays a web page using WebView.
2. The app handles geolocation permissions and file upload requests from the web content.
3. When clicking the "Choose File" button in the web form:
   - You can choose an image from the device's gallery.
   - You can capture an image using the device's camera.
4. The chosen or captured image will be passed to the web form for uploading.
5. If you encounter any issues or errors, refer to the troubleshooting section in this README.

## Troubleshooting

If you encounter any issues or errors while using the app, consider the following steps:

- Ensure that the app has the required permissions, including camera, gallery, internet access, and location permissions.
- Check the app's internet connection to ensure proper loading of web content.

## Contributing

Contributions are welcome! If you encounter any bugs or have suggestions for improvements, feel free to open an issue or submit a pull request.
