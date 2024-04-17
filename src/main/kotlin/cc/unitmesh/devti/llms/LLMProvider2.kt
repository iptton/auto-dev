package cc.unitmesh.devti.llms

import cc.unitmesh.cf.core.llms.LlmMsg
import cc.unitmesh.devti.gui.chat.ChatRole
import cc.unitmesh.devti.llms.MessageStatus.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * LLMProvider provide only session-free interfaces
 *
 * It's LLMProvider's responsibility to maintain the network connection
 * But the chat session is maintained by the client
 *
 * The implementations should provide a way to convert the response to a ChatSession
 */
abstract class LLMProvider2 {

    final fun textComplete(session: ChatSession): Flow<ChatSession> = callbackFlow {

        // 先添加一条空消息，用于标记会话开始
        val newSession = session.addNewMessage(ChatMessage(content = "", role = ChatRole.Assistant))

        stream(session).collect { responseBody ->
            val chatSession = responseToChatSession(session, responseBody)
            send(chatSession)
        }

        awaitClose()
    }

    final fun imageGenerate(prompt: String): Flow<ChatSession> {
        TODO()
    }

    /**
     * 从 LLM 获取消息
     *
     * 各 LLM 的返回消息格式不同，此方法需解析出 LLM 返回的实际消息，不包括 token 数等其他信息
     *
     * @param sendingSession 发送消息的会话，sendingSession 初始时最后的一条消息是占位消息。
     * @return 返回消息的流
     */
    protected abstract fun stream(sendingSession: ChatSession): Flow<String>

    /**
     * 将 LLM 返回的消息组合成 ChatSession。
     *
     * @param sendingSession 发送消息的会话,sendingSession 初始时最后的一条消息是占位消息。新消息应直接在其基础上修改。
     * @param responseBody LLM 返回的解析出来的消息，不包含 token 数等其他信息，由 [stream] 方法传递过来
     */
    protected open fun responseToChatSession(sendingSession: ChatSession, responseBody: String): ChatSession {
        return sendingSession.appendToLastMessage(responseBody)
    }
}

/**
 * 后台返回消息状态：[BEGIN] 刚开始返回 [CONTENT] 中间内容 [END] 内容结束
 */
enum class MessageStatus {
    BEGIN, CONTENT, END
}

data class ChatSession(
    val conversionName: String,
    val chatHistory: List<ChatMessage>,
    /**
     * 当前会话状态，为 [MessageStatus.END] 时才允许发下一个对话
     */
    var status: MessageStatus = END,
)

fun ChatSession.isEnd() = status == END

/**
 * 为原会话追加一条新消息生成一个新的会话，并标记状态为 [MessageStatus.BEGIN]
 */
fun ChatSession.addNewMessage(message: ChatMessage): ChatSession = this.copy(chatHistory = chatHistory + message, status = BEGIN)

/**
 * 更新最后一条消息，并标记状态为 [MessageStatus.CONTENT]
 */
fun ChatSession.appendToLastMessage(message: String): ChatSession {
    val lastMessage = chatHistory.last()
    val newMessage = lastMessage.copy(content = lastMessage.content + message)
    return this.copy(chatHistory = chatHistory.dropLast(1) + newMessage, status = CONTENT)
}

data class ChatMessage(
    val content: String,
    val role: ChatRole,
)
