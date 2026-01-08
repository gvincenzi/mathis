document.addEventListener('DOMContentLoaded', () => {
    const chatMessages = document.getElementById('chat-messages');
    const chatInput = document.getElementById('chat-input');
    const sendButton = document.getElementById('send-button');

    let conversationId = localStorage.getItem('mathisConversationId');
    if (!conversationId) {
        conversationId = crypto.randomUUID();
        localStorage.setItem('mathisConversationId', conversationId);
    }

    function displayMessage(message, sender) {
        const messageElement = document.createElement('div');
        messageElement.classList.add('chat-message', 'p-2', 'rounded-3', 'mb-2');

        if (sender === 'user') {
            messageElement.classList.add('user-message', 'ms-auto');
            messageElement.textContent = message;
        } else { // Messaggio AI
            messageElement.classList.add('ai-message', 'me-auto');
            // Usa DOMPurify se incluso
            if (window.DOMPurify) {
                messageElement.innerHTML = DOMPurify.sanitize(marked.parse(message));
            } else {
                messageElement.innerHTML = marked.parse(message);
            }
        }
        chatMessages.appendChild(messageElement);
        saveChatHistory();
        scrollToBottom();
    }

    function scrollToBottom() {
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }

    function saveChatHistory() {
        localStorage.setItem('mathisChatHistory_' + conversationId, chatMessages.innerHTML);
    }
    function loadChatHistory() {
        chatMessages.innerHTML = localStorage.getItem('mathisChatHistory_' + conversationId) || '';
    }

    async function sendMessage() {
        const messageText = chatInput.value.trim();
        if (!messageText) return;

        displayMessage(messageText, 'user');
        chatInput.value = '';
        sendButton.disabled = true;

        const loadingIndicator = document.createElement('div');
        loadingIndicator.classList.add('loading-indicator', 'ai-message', 'me-auto');
        loadingIndicator.textContent = 'Mathis is typing...';
        chatMessages.appendChild(loadingIndicator);
        scrollToBottom();

        const chatMessage = {
            conversationId: conversationId,
            userType: 'HUMAN',
            body: messageText
        };

        try {
            const response = await fetch('/api/chat', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(chatMessage)
            });

            chatMessages.removeChild(loadingIndicator);

            if (response.ok) {
                const data = await response.json();
                displayMessage(data.body, 'ai');
            } else {
                displayMessage('Error: Could not get a response from Mathis.', 'ai');
                console.error('API Error:', response.status, response.statusText);
            }
        } catch (error) {
            chatMessages.removeChild(loadingIndicator);
            displayMessage('Error: Network issue or server not reachable.', 'ai');
            console.error('Fetch Error:', error);
        } finally {
            sendButton.disabled = false;
            scrollToBottom();
            chatInput.focus();
        }
    }

    sendButton.addEventListener('click', sendMessage);

    chatInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });

    // -------------------------
    // Cronologia e benvenuto
    loadChatHistory();
    if (!localStorage.getItem('mathisWelcomed_' + conversationId)) {
        displayMessage("Hello! I'm Mathis, your personal AI assistant. How can I help you today?", 'ai');
        localStorage.setItem('mathisWelcomed_' + conversationId, '1');
    }
});
