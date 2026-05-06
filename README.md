# PixelAura — Android Social Platform for Artists

PixelAura is a production-ready Android social platform built natively in **Kotlin**, designed for artists to share their work, grow an audience, and engage with a creative community. The application implements the core architecture of a modern social media system — including a real-time notification pipeline, a bidirectional social graph, an in-app messaging system, and a full content engagement suite.

---

## Features

- **Authentication** — Secure user registration, login, and email-based password recovery via Firebase Authentication
- **Post & Artwork Sharing** — Users can create posts, upload artwork, and make content available for community download
- **Social Graph** — Bidirectional user following system with personalized content feeds based on follow relationships
- **Engagement** — Full like and repost system with engagement metrics tracked per post
- **Direct Messaging** — Real-time in-app messaging between users powered by Firestore snapshot listeners for instant message delivery
- **Notifications** — Event-driven notification system that triggers on follows, likes, reposts, and messages using Firestore real-time updates
- **User Profiles** — Fully customizable profiles with artwork portfolios and social stats

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| Platform | Android (Native) |
| Authentication | Firebase Authentication |
| Database | Cloud Firestore (NoSQL) |
| Image Hosting | Imgur API |
| Real-time Updates | Firestore Snapshot Listeners |

---

## Architecture & Technical Details

**NoSQL Data Modeling**
Cloud Firestore serves as the primary database, modeling complex relational data across multiple collections — users, posts, likes, reposts, follow relationships, messaging threads, and notifications. The schema is designed to minimize read operations given free-tier quota constraints.

**Real-time System**
Both the direct messaging system and the notification pipeline leverage Firestore's snapshot listeners, allowing the UI to react to data changes instantly without polling — mirroring the event-driven architecture used in production social platforms.

**Image Pipeline**
User-uploaded artwork is handled through the Imgur API, decoupling image storage from Firestore and avoiding binary data in the database. This keeps Firestore read/write costs low while enabling efficient image delivery.

**Social Graph**
The follow system is implemented as a bidirectional relationship model, enabling personalized feed generation based on a user's following list and powering the notification triggers for social interactions.

---

## Limitations

This application runs on free-tier cloud services and operates within platform-imposed quotas on Firestore read/write operations and Firebase Authentication requests. Users are advised to register with a valid email address to ensure access to password recovery features.

> **UI Note:** The current interface is optimized for light mode. Dark mode support is not fully implemented in this version.

---
## 📦 Download APK

[![Download](https://img.shields.io/badge/Download-APK-blue?style=for-the-badge)](https://github.com/franchescaLei/PixelAura/releases/download/v1.0/PixelAura.apk)
