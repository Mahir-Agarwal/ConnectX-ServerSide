const WebSocket = require('ws');
const axios = require('axios');

const BASE_URL = 'http://localhost:8080';
const WS_URL = 'ws://localhost:8080/ws';

async function runTest() {
    console.log(" Starting End-to-End Signaling Test...");

    try {
        // 1. Create Session
        console.log("\n Creating Session...");
        const sessionRes = await axios.post(`${BASE_URL}/api/sessions`);
        const { sessionId, senderToken, receiverToken } = sessionRes.data;
        console.log(`    Session Created: ${sessionId}`);
        console.log(`    Sender Token: ${senderToken.substring(0, 10)}...`);
        console.log(`    Receiver Token: ${receiverToken.substring(0, 10)}...`);

        // 2. Connect Sender
        console.log("\n Connecting Sender...");
        const senderWs = new WebSocket(`${WS_URL}?token=${senderToken}`);

        await new Promise((resolve, reject) => {
            senderWs.on('open', () => {
                console.log("    Sender Connected");
                resolve();
            });
            senderWs.on('error', (err) => reject("Sender WS Error: " + err));
        });

        // 3. Sender Joins
        console.log("\n Sender Sending JOIN_AS_SENDER...");
        senderWs.send(JSON.stringify({ type: 'JOIN_AS_SENDER', sessionId }));

        // Wait for server to process Sender Join
        await new Promise(r => setTimeout(r, 1000));

        // 4. Connect Receiver
        console.log("\n Connecting Receiver...");
        const receiverWs = new WebSocket(`${WS_URL}?token=${receiverToken}`);

        await new Promise((resolve, reject) => {
            receiverWs.on('open', () => {
                console.log("    Receiver Connected");
                resolve();
            });
            receiverWs.on('error', (err) => reject("Receiver WS Error: " + err));
        });

        // 5. Receiver Joins & Waits for ACK
        console.log("\n Receiver Sending JOIN_AS_RECEIVER...");
        receiverWs.send(JSON.stringify({ type: 'JOIN_AS_RECEIVER', sessionId }));

        // Setup Listeners for Verification
        const senderPromise = new Promise((resolve, reject) => {
            senderWs.on('message', (data) => {
                const msg = JSON.parse(data);
                console.log(`    Sender Received: ${msg.type}`);
                if (msg.type === 'ERROR') {
                    console.log("    Error Details:", JSON.stringify(msg));
                }
                if (msg.type === 'RECEIVER_JOINED') {
                    console.log("    Verified: Sender got RECEIVER_JOINED");
                    resolve();
                }
            });
        });

        const receiverAckPromise = new Promise((resolve, reject) => {
            receiverWs.on('message', (data) => {
                const msg = JSON.parse(data);
                console.log(`    Receiver Received: ${msg.type}`);
                if (msg.type === 'JOIN_ACK') {
                    console.log("    Verified: Receiver got JOIN_ACK");
                    resolve();
                }
            });
        });

        await Promise.all([senderPromise, receiverAckPromise]);

        // 6. Test Offer/Answer Flow
        console.log("\n Testing Offer/Answer Flow...");

        // Sender sends OFFER
        console.log("    Sender sending OFFER...");
        senderWs.send(JSON.stringify({
            type: 'OFFER',
            sessionId,
            payload: { sdp: 'mock-sdp-offer' }
        }));

        // Receiver waits for OFFER
        await new Promise((resolve) => {
            receiverWs.once('message', (data) => {
                const msg = JSON.parse(data);
                if (msg.type === 'OFFER') {
                    console.log("    Verified: Receiver got OFFER");
                    resolve();
                }
            });
        });

        // Receiver sends ANSWER
        console.log("    Receiver sending ANSWER...");
        receiverWs.send(JSON.stringify({
            type: 'ANSWER',
            sessionId,
            payload: { sdp: 'mock-sdp-answer' }
        }));

        // Sender waits for ANSWER
        await new Promise((resolve) => {
            senderWs.once('message', (data) => {
                const msg = JSON.parse(data);
                if (msg.type === 'ANSWER') {
                    console.log("    Verified: Sender got ANSWER");
                    resolve();
                }
            });
        });

        console.log("\n TEST PASSED SUCCESSFULLY! ");
        senderWs.close();
        receiverWs.close();

    } catch (error) {
        console.error("\n TEST FAILED:", error.message || error);
        if (error.response) {
            console.error("   Response Data:", error.response.data);
        }
        process.exit(1);
    }
}

runTest();
