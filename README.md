# PixelAura

This project was developed as the final requirement for my Mobile Systems and Technologies course.

PixelAura is a lightweight social platform designed primarily for artists to share their work and collaborate with others. The application enables users to create posts, showcase artwork, and engage with the community through likes, reposts, and downloadable content. It also includes core social features such as user following, direct messaging, notifications, and customizable user profiles. While intentionally scoped as a foundational implementation, the platform demonstrates the essential components of a modern social media application.

From a technical perspective, the application leverages Firebase services for backend functionality. Firebase Authentication is used to handle secure user registration, login, and password reset via email. Cloud Firestore serves as the primary database for managing user data, posts, and interactions. Image hosting and delivery are handled via Imgur, allowing efficient storage and retrieval of user-uploaded artwork.

Due to reliance on free-tier cloud services, the application operates within platform-imposed limits (e.g., read/write operations and authentication quotas). Users are advised to register with a valid email address to ensure full access to account recovery features.

Note: The current UI is optimized for light mode. For the best visual experience, it is recommended to use the application with your device set to light theme, as dark mode support is not fully implemented in this version.
## 📦 Download APK

[![Download](https://img.shields.io/badge/Download-APK-blue?style=for-the-badge)](https://github.com/franchescaLei/PixelAura/releases/download/v1.0/PixelAura.apk)
