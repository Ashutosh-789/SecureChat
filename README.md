# 🔐 SecureChat

A secure real-time messaging application developed in **Java** using **Socket Programming**, **Java Swing**, and a **hybrid cryptographic approach** combining **RSA** and **DES** for end-to-end encrypted communication.

---

## 📖 Overview

SecureChat enables two users to exchange messages securely over a network. The application uses **RSA (2048-bit)** to securely exchange a **DES session key**, after which all chat messages are encrypted using **DES/CBC/PKCS5Padding** before transmission.

The project demonstrates the implementation of **hybrid encryption**, **client-server communication**, and **desktop GUI development**.

---

## ✨ Features

- End-to-End Encrypted Messaging
- RSA-2048 Secure Key Exchange
- DES/CBC/PKCS5Padding Message Encryption
- Real-Time Client-Server Communication
- Java Swing GUI
- Multiple Client Support
- Socket Programming using TCP
- Encryption & Decryption Visualization
- Secure Message Transmission

---

## 🛠 Technologies Used

- Java
- Java Swing
- Java Socket Programming
- RSA Cryptography
- DES Encryption
- Object Serialization
- Multithreading

---

## 📂 Project Structure

```text
SecureChat/
│
├── src/
│   └── securechat/
│       ├── Client.java
│       ├── Server.java
│       ├── Message.java
│       ├── RSAUtil.java
│       ├── DESUtil.java
│       └── EncryptionDialog.java
│
├── screenshots/
│   ├── Client.png
│   ├── Server.png
│   ├── Login.png
│   ├── Alice Keys.png
│   ├── Bob Keys.png
│   ├── Chat.png
│   │
│   ├── Encryption/
│   │   ├── Plaintext.png
│   │   ├── DES_Encryption.png
│   │   ├── RSA_Key_Wrapping.png
│   │   └── Ciphertext.png
│   │
│   └── Decryption/
│       ├── Received_Ciphertext.png
│       ├── RSA_Key_Unwrapping.png
│       └── DES_Decryption.png
│
├── README.md
└── .gitattributes
```

---

## 🔐 Encryption Workflow

```text
Sender
   │
   │ Generate DES Session Key
   ▼
Encrypt Message using DES
   │
Encrypt DES Key using Receiver's RSA Public Key
   │
Send Ciphertext + Encrypted DES Key
   │
────────────── Network ──────────────
   │
Receiver decrypts DES Key using RSA Private Key
   │
Decrypt Ciphertext using DES
   ▼
Original Message
```

---

## 🚀 How to Run

### 1. Clone the Repository

```bash
git clone https://github.com/Ashutosh-789/SecureChat.git
```

### 2. Open the Project

Open the project in **Eclipse** or **IntelliJ IDEA**.

### 3. Run the Server

Run:

```text
Server.java
```

### 4. Run Client 1

Run:

```text
Client.java
```

Enter a username.

### 5. Run Client 2

Run:

```text
Client.java
```

Enter another username.

### 6. Start Chatting

Send encrypted messages between clients.

Click on any message bubble to view the complete encryption and decryption process.

---

## 📚 Learning Outcomes

- Java Socket Programming
- Client-Server Architecture
- Hybrid Encryption (RSA + DES)
- Secure Key Exchange
- Multithreading
- Java Swing GUI Development
- Secure Network Communication

---

## 👨‍💻 Author

**Ashutosh Baliarsingh**

- GitHub: https://github.com/Ashutosh-789
- LinkedIn: https://linkedin.com/in/ashutosh-baliarsingh-2539432a1

---

⭐ **If you found this project useful, consider giving it a star!**