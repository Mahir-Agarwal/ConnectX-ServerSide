const WebSocket = require('ws');
const axios = require('axios');

// Configuration
// Configuration
const GATEWAY_URL = 'https://committable-chong-desperately.ngrok-free.dev';
const HTTP_BASE = `${GATEWAY_URL}/api/sessions`;
const WS_BASE = `wss://committable-chong-desperately.ngrok-free.dev/ws`;

// Colors for logging
const colors = {
    reset: "\x1b[0m",
    green: "\x1b[32m",
    red: "\x1b[31m",
    yellow: "\x1b[33m",
    blue: "\x1b[34m",
    magenta: "\x1b[35m",
    cyan: "\x1b[36m"
};

function log(msg, color = colors.reset) {
    console.log(`${color}${msg}${colors.reset}`);
}

async function runTest() {
    log("\n STARTING CONNECTX E2E TEST SUITE\n", colors.cyan);

    let sessionId, senderToken, receiverToken;
    let senderWs, receiverWs;

    try {
        // --- STEP 1: CREATE SESSION ---
        log("  Creating Session via Gateway...", colors.yellow);
        const sessionRes = await axios.post(HTTP_BASE);

        if (sessionRes.status === 200) {
            sessionId = sessionRes.data.sessionId;
            senderToken = sessionRes.data.senderToken;
            receiverToken = sessionRes.data.receiverToken;
            log(`    Session Created: ${sessionId}`, colors.green);
            log(`      Sender Token:   ${senderToken.substring(0, 15)}...`, colors.reset);
            log(`      Receiver Token: ${receiverToken.substring(0, 15)}...`, colors.reset);
        } else {
            throw new Error(`Failed to create session. Status: ${sessionRes.status}`);
        }

        // --- STEP 2: CONNECT SENDER ---
        log("\n  Connecting SENDER to Signaling Service...", colors.yellow);
        senderWs = new WebSocket(`${WS_BASE}?token=${senderToken}`);

        await new Promise((resolve, reject) => {
            senderWs.on('open', resolve);
            senderWs.on('error', reject);
        });
        log("    Sender WebSocket Connected", colors.green);

        // Sender sends JOIN
        log("    Sender sending JOIN_AS_SENDER...", colors.blue);
        senderWs.send(JSON.stringify({ type: 'JOIN_AS_SENDER', sessionId }));

        // --- STEP 3: CONNECT RECEIVER ---
        log("\n  Connecting RECEIVER to Signaling Service...", colors.yellow);
        receiverWs = new WebSocket(`${WS_BASE}?token=${receiverToken}`);

        await new Promise((resolve, reject) => {
            receiverWs.on('open', resolve);
            receiverWs.on('error', reject);
        });
        log("    Receiver WebSocket Connected", colors.green);

        // Receiver sends JOIN
        log("    Receiver sending JOIN_AS_RECEIVER...", colors.blue);
        receiverWs.send(JSON.stringify({ type: 'JOIN_AS_RECEIVER', sessionId }));


        // --- STEP 4: VERIFY JOIN HANDSHAKE ---
        log("\n  Verifying Join Handshake...", colors.yellow);

        const handshakePromise = new Promise((resolve, reject) => {
            let senderVerified = false;
            let receiverVerified = false;

            const checkDone = () => {
                if (senderVerified && receiverVerified) resolve();
            };

            // Sender should receive RECEIVER_JOINED
            senderWs.on('message', (data) => {
                const msg = JSON.parse(data);
                if (msg.type === 'RECEIVER_JOINED') {
                    log("    Sender received: RECEIVER_JOINED", colors.magenta);
                    senderVerified = true;
                    checkDone();
                } else if (msg.type === 'ERROR') {
                    log(`    Sender Error: ${msg.payload}`, colors.red);
                }
            });

            // Receiver should receive JOIN_ACK
            receiverWs.on('message', (data) => {
                const msg = JSON.parse(data);
                if (msg.type === 'JOIN_ACK') {
                    log("    Receiver received: JOIN_ACK", colors.magenta);
                    receiverVerified = true;
                    checkDone();
                } else if (msg.type === 'ERROR') {
                    log(`    Receiver Error: ${msg.payload}`, colors.red);
                }
            });

            setTimeout(() => reject(new Error("Timeout waiting for Handshake")), 5000);
        });

        await handshakePromise;
        log("    Handshake Verified!", colors.green);


        // --- STEP 5: SIGNALING EXCHANGE (OFFER/ANSWER) ---
        log("\n  Testing Signaling Exchange...", colors.yellow);

        // Sender sends OFFER
        const offerPayload = { sdp: "v=0\r\n...", type: "offer" };
        log("    Sender sending OFFER...", colors.blue);
        senderWs.send(JSON.stringify({
            type: 'OFFER',
            sessionId,
            payload: JSON.stringify(offerPayload)
        }));

        // Receiver expects OFFER
        await new Promise((resolve, reject) => {
            const handler = (data) => {
                const msg = JSON.parse(data);
                if (msg.type === 'OFFER') {
                    log("    Receiver received: OFFER", colors.magenta);
                    receiverWs.off('message', handler); // Clean up
                    resolve();
                }
            };
            receiverWs.on('message', handler);
            setTimeout(() => reject(new Error("Timeout waiting for OFFER")), 5000);
        });

        // Receiver sends ANSWER
        const answerPayload = { sdp: "v=0\r\n...", type: "answer" };
        log("    Receiver sending ANSWER...", colors.blue);
        receiverWs.send(JSON.stringify({
            type: 'ANSWER',
            sessionId,
            payload: JSON.stringify(answerPayload)
        }));

        // Sender expects ANSWER
        await new Promise((resolve, reject) => {
            const handler = (data) => {
                const msg = JSON.parse(data);
                if (msg.type === 'ANSWER') {
                    log("    Sender received: ANSWER", colors.magenta);
                    senderWs.off('message', handler);
                    resolve();
                }
            };
            senderWs.on('message', handler);
            setTimeout(() => reject(new Error("Timeout waiting for ANSWER")), 5000);
        });

        log("    Signaling Exchange Verified!", colors.green);

        // --- STEP 6: ICE CANDIDATE EXCHANGE ---
        log("\n  Testing ICE Candidate Exchange...", colors.yellow);

        // Sender sends ICE CANDIDATE
        const icePayloadSender = { candidate: "candidate:123...", sdpMid: "0", sdpMLineIndex: 0 };
        log("    Sender sending ICE_CANDIDATE...", colors.blue);
        senderWs.send(JSON.stringify({
            type: 'ICE_CANDIDATE',
            sessionId,
            payload: JSON.stringify(icePayloadSender)
        }));

        // Receiver expects ICE_CANDIDATE
        await new Promise((resolve, reject) => {
            const handler = (data) => {
                const msg = JSON.parse(data);
                if (msg.type === 'ICE_CANDIDATE') {
                    log("    Receiver received: ICE_CANDIDATE", colors.magenta);
                    receiverWs.off('message', handler);
                    resolve();
                }
            };
            receiverWs.on('message', handler);
            setTimeout(() => reject(new Error("Timeout waiting for Sender's ICE_CANDIDATE")), 5000);
        });

        // Receiver sends ICE CANDIDATE
        const icePayloadReceiver = { candidate: "candidate:456...", sdpMid: "0", sdpMLineIndex: 0 };
        log("    Receiver sending ICE_CANDIDATE...", colors.blue);
        receiverWs.send(JSON.stringify({
            type: 'ICE_CANDIDATE',
            sessionId,
            payload: JSON.stringify(icePayloadReceiver)
        }));

        // Sender expects ICE_CANDIDATE
        await new Promise((resolve, reject) => {
            const handler = (data) => {
                const msg = JSON.parse(data);
                if (msg.type === 'ICE_CANDIDATE') {
                    log("    Sender received: ICE_CANDIDATE", colors.magenta);
                    senderWs.off('message', handler);
                    resolve();
                }
            };
            senderWs.on('message', handler);
            setTimeout(() => reject(new Error("Timeout waiting for Receiver's ICE_CANDIDATE")), 5000);
        });

        log("    ICE Candidate Exchange Verified!", colors.green);
        log("\n ALL SYSTEMS GO! TEST PASSED. ", colors.green);

    } catch (err) {
        log(`\n TEST FAILED: ${err.message}`, colors.red);
        if (err.response) {
            log(`   Status: ${err.response.status}`, colors.red);
            log(`   Data: ${JSON.stringify(err.response.data)}`, colors.red);
        }
        process.exit(1);
    } finally {
        if (senderWs) senderWs.close();
        if (receiverWs) receiverWs.close();
    }
}

runTest();
