package com.whatsapp.clone.chat.web;

import com.whatsapp.clone.chat.client.PresenceClient;
import com.whatsapp.clone.chat.domain.Chat;
import com.whatsapp.clone.chat.domain.ChatMember;
import com.whatsapp.clone.chat.domain.Message;
import com.whatsapp.clone.chat.repo.ChatMemberRepository;
import com.whatsapp.clone.chat.repo.ChatRepository;
import com.whatsapp.clone.chat.repo.MessageRepository;
import com.whatsapp.clone.chat.util.JwtUtil;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatRepository chats;
    private final ChatMemberRepository members;
    private final MessageRepository messages;
    private final SimpMessagingTemplate broker;
    private final PresenceClient presence;

    public ChatController(ChatRepository c, ChatMemberRepository m, MessageRepository msg,
                          SimpMessagingTemplate b, PresenceClient p) {
        this.chats = c;
        this.members = m;
        this.messages = msg;
        this.broker = b;
        this.presence = p;
    }

    private UserCtx user(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new RuntimeException("Unauthorized");
        }
        String token = auth.substring(7);
        String secret = System.getenv().getOrDefault("JWT_SECRET", "devsecret");
        Map<String, Object> map = JwtUtil.parse(token, secret);
        return new UserCtx(UUID.fromString((String) map.get("sub")), (String) map.get("displayName"));
    }

    @GetMapping
    public List<Map<String, ? extends Serializable>> list(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        UserCtx u = user(auth);
        List<ChatMember> ms = members.findByUserId(u.getId());
        Set<UUID> chatIds = ms.stream().map(ChatMember::getChatId).collect(Collectors.toSet());

        Iterable<Chat> iterable = chats.findAllById(chatIds);
        List<Chat> cs = new ArrayList<>();
        for (Chat c : iterable) cs.add(c);

        List<Map<String, ? extends Serializable>> out = new ArrayList<>();
        for (Chat c : cs) {
            Map<String, Serializable> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("type", c.getType().name());
            m.put("name", c.getName());
            out.add(m);
        }
        return out;
    }

    @PostMapping("/dm/{otherId}")
    public Map<String, Object> dm(@RequestHeader("Authorization") String auth,
                                  @PathVariable String otherId) {
        UserCtx u = user(auth);
        UUID other = UUID.fromString(otherId);

        List<ChatMember> myMs = members.findByUserId(u.getId());
        Set<UUID> my = myMs.stream().map(ChatMember::getChatId).collect(Collectors.toSet());
        for (UUID cid : my) {
            List<ChatMember> list = members.findByChatId(cid);
            boolean hasBoth = list.size() == 2 && list.stream().anyMatch(m -> m.getUserId().equals(other));
            if (hasBoth) {
                Chat c = chats.findById(cid).orElseThrow(IllegalStateException::new);
                Map<String, Object> resp = new HashMap<>();
                resp.put("id", c.getId());
                resp.put("type", c.getType().name());
                return resp;
            }
        }

        Chat c = new Chat();
        c.setType(Chat.ChatType.DIRECT);
        c.setCreatedBy(u.getId());
        chats.save(c);

        ChatMember m1 = new ChatMember();
        m1.setChatId(c.getId());
        m1.setUserId(u.getId());
        m1.setRole(ChatMember.Role.ADMIN);
        members.save(m1);

        ChatMember m2 = new ChatMember();
        m2.setChatId(c.getId());
        m2.setUserId(other);
        m2.setRole(ChatMember.Role.MEMBER);
        members.save(m2);

        Map<String, Object> created = new HashMap<>();
        created.put("id", c.getId());
        created.put("type", "DIRECT");
        return created;
    }

    @PostMapping("/group")
    public Map<String, Object> group(@RequestHeader("Authorization") String auth,
                                     @RequestBody CreateGroup req) {
        UserCtx u = user(auth);
        Chat c = new Chat();
        c.setType(Chat.ChatType.GROUP);
        c.setName(req.name == null ? "New Group" : req.name);
        c.setCreatedBy(u.getId());
        chats.save(c);

        ChatMember owner = new ChatMember();
        owner.setChatId(c.getId());
        owner.setUserId(u.getId());
        owner.setRole(ChatMember.Role.ADMIN);
        members.save(owner);

        if (req.members != null) {
            for (String mid : req.members) {
                ChatMember m = new ChatMember();
                m.setChatId(c.getId());
                m.setUserId(UUID.fromString(mid));
                m.setRole(ChatMember.Role.MEMBER);
                members.save(m);
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", c.getId());
        resp.put("type", "GROUP");
        resp.put("name", c.getName());
        return resp;
    }

    @PostMapping("/{chatId}/members/{userId}")
    public ResponseEntity<?> add(@RequestHeader("Authorization") String auth,
                                 @PathVariable String chatId,
                                 @PathVariable String userId) {
        UserCtx u = user(auth);
        UUID cid = UUID.fromString(chatId);
        UUID uid = UUID.fromString(userId);

        ChatMember me = members.findByChatIdAndUserId(cid, u.getId()).orElse(null);
        if (me == null || me.getRole() != ChatMember.Role.ADMIN) return ResponseEntity.status(403).build();
        if (members.findByChatIdAndUserId(cid, uid).isPresent())
            return ResponseEntity.ok(Collections.singletonMap("status", "exists"));

        ChatMember m = new ChatMember();
        m.setChatId(cid);
        m.setUserId(uid);
        m.setRole(ChatMember.Role.MEMBER);
        members.save(m);
        return ResponseEntity.ok(Collections.singletonMap("status", "added"));
    }

    @DeleteMapping("/{chatId}/members/{userId}")
    public ResponseEntity<?> remove(@RequestHeader("Authorization") String auth,
                                    @PathVariable String chatId,
                                    @PathVariable String userId) {
        UserCtx u = user(auth);
        UUID cid = UUID.fromString(chatId);
        UUID uid = UUID.fromString(userId);

        ChatMember me = members.findByChatIdAndUserId(cid, u.getId()).orElse(null);
        if (me == null || me.getRole() != ChatMember.Role.ADMIN) return ResponseEntity.status(403).build();

        ChatMember m = members.findByChatIdAndUserId(cid, uid).orElse(null);
        if (m == null) return ResponseEntity.notFound().build();

        members.delete(m);
        return ResponseEntity.ok(Collections.singletonMap("status", "removed"));
    }

    @GetMapping("/{chatId}/messages")
    public List<Message> history(@RequestHeader("Authorization") String auth,
                                 @PathVariable String chatId) {
        UserCtx u = user(auth);
        UUID cid = UUID.fromString(chatId);
        if (members.findByChatIdAndUserId(cid, u.getId()).isEmpty()) throw new RuntimeException("Forbidden");

        List<Message> list = messages.findTop50ByChatIdOrderByCreatedAtDesc(cid);
        Collections.reverse(list);
        return list;
    }

    @PostMapping("/{chatId}/message")
    public Map<String, Object> send(@RequestHeader("Authorization") String auth,
                                    @PathVariable String chatId,
                                    @RequestBody SendReq req) {
        UserCtx u = user(auth);
        UUID cid = UUID.fromString(chatId);
        if (members.findByChatIdAndUserId(cid, u.getId()).isEmpty()) throw new RuntimeException("Forbidden");

        Message saved = messages.save(new Message(cid, u.getId(), u.getDisplay(), req.content));

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", saved.getId());
        payload.put("chatId", cid.toString());
        payload.put("senderId", u.getId().toString());
        payload.put("senderName", u.getDisplay());
        payload.put("content", saved.getContent());
        payload.put("createdAt", saved.getCreatedAt().toString());

        broker.convertAndSend("/topic/chat/" + cid, payload);
        return payload;
    }

    @GetMapping("/pingPresence/{userId}")
    @CircuitBreaker(name = "presence", fallbackMethod = "presenceFallback")
    @Retry(name = "presence")
    @Bulkhead(name = "presence", type = Bulkhead.Type.THREADPOOL)
    public Map<String, Object> pingPresence(@PathVariable String userId) {
        return presence.get(userId);
    }

    public Map<String, Object> presenceFallback(String userId, Throwable t) {
        Map<String, Object> m = new HashMap<>();
        m.put("userId", userId);
        m.put("status", "unknown");
        m.put("fallback", true);
        return m;
    }

    private static class UserCtx {
        private final UUID id;
        private final String display;

        public UserCtx(UUID id, String display) {
            this.id = id;
            this.display = display;
        }

        public UUID getId() {
            return id;
        }

        public String getDisplay() {
            return display;
        }
    }

    static class CreateGroup {
        public String name;
        public List<String> members;
    }

    static class SendReq {
        public String content;
    }
}
