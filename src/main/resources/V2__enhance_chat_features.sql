-- Add indexes for better performance
CREATE INDEX IF NOT EXISTS idx_chat_conversations_user_updated
    ON chat_conversations(user_id, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_chat_messages_conversation_created
    ON chat_messages(conversation_id, created_at ASC);

-- Add conversation metadata
ALTER TABLE chat_conversations
    ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}';

-- Add message type for better categorization
ALTER TABLE chat_messages
    ADD COLUMN IF NOT EXISTS message_type VARCHAR(20) DEFAULT 'text';

-- Update existing image messages
UPDATE chat_messages
SET message_type = 'image'
WHERE content LIKE 'https://image.pollinations.ai/%';