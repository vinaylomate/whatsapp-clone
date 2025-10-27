import React, { useEffect, useMemo, useRef, useState } from 'react'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'
const BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8088'
type Chat = { id: string; type: 'DIRECT'|'GROUP'; name?: string }
type Msg = { id:number; chatId:string; senderId:string; senderName:string; content:string; createdAt:string }
export default function App() {
  const [token, setToken] = useState(localStorage.getItem('jwt') || '')
  const [me, setMe] = useState<{id:string; email:string; displayName:string} | null>(null)
  const [chats, setChats] = useState<Chat[]>([])
  const [active, setActive] = useState<Chat | null>(null)
  const [messages, setMessages] = useState<Msg[]>([])
  const [newMsg, setNewMsg] = useState('')
  const [dmTarget, setDmTarget] = useState('') // other user's UUID
  const [groupName, setGroupName] = useState('')
  const [groupMembers, setGroupMembers] = useState('') // comma-separated UUIDs
  const clientRef = useRef<Client | null>(null)
  const headers = useMemo(() => ({ 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` }), [token])

  useEffect(() => {
    if (!active) return
    const sock = new SockJS(`${BASE}/ws`)
    const client = new Client({ webSocketFactory: () => sock as any, reconnectDelay: 3000 })
    client.onConnect = () => { client.subscribe(`/topic/chat/${active.id}`, (frame) => { const msg = JSON.parse(frame.body); setMessages((prev)=> [...prev, msg]) }) }
    client.activate(); clientRef.current = client; return () => client.deactivate()
  }, [active?.id])

  const login = async (email:string, password:string) => {
    const r = await fetch(`${BASE}/api/auth/login`, { method:'POST', headers:{'Content-Type':'application/json', 'JWT_SECRET':'devsecret'}, body: JSON.stringify({ email, password }) })
    const j = await r.json(); if(j.token){ localStorage.setItem('jwt', j.token); setToken(j.token); setMe(j.user) }
  }
  const register = async (email:string, password:string, displayName:string) => {
    await fetch(`${BASE}/api/auth/register`, { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({ email, password, displayName }) })
  }
  const loadChats = async () => { const r = await fetch(`${BASE}/api/chat`, { headers }); const j = await r.json(); setChats(j); if (!active && j[0]) openChat(j[0]) }
  const openChat = async (chat:Chat) => { setActive(chat); const r = await fetch(`${BASE}/api/chat/${chat.id}/messages`, { headers }); setMessages(await r.json()) }
  const send = async () => { if(!active) return; const r = await fetch(`${BASE}/api/chat/${active.id}/message`, { method:'POST', headers, body: JSON.stringify({ content: newMsg }) }); setNewMsg('') }
  const createDM = async () => { const r = await fetch(`${BASE}/api/chat/dm/${dmTarget}`, { method:'POST', headers }); const c = await r.json(); setDmTarget(''); loadChats(); }
  const createGroup = async () => {
    const members = groupMembers.split(',').map(s=>s.trim()).filter(Boolean)
    const r = await fetch(`${BASE}/api/chat/group`, { method:'POST', headers, body: JSON.stringify({ name: groupName, members }) })
    setGroupName(''); setGroupMembers(''); loadChats()
  }

  return (
    <div style={{ fontFamily:'system-ui, sans-serif', display:'grid', gridTemplateColumns:'280px 1fr', height:'100vh' }}>
      <div style={{ padding:12, borderRight:'1px solid #ddd' }}>
        <h2>Whatsapp Clone</h2>
        {!token && (
          <AuthPanel onLogin={login} onRegister={register} />
        )}
        {token && (
          <div>
            <button onClick={loadChats}>Load Chats</button>
            <div style={{ marginTop:12 }}>
              {chats.map(c => (
                <div key={c.id} style={{ padding:8, borderRadius:8, background: active?.id===c.id ? '#eef' : 'transparent', cursor:'pointer' }} onClick={()=>openChat(c)}>
                  <b>{c.type === 'DIRECT' ? 'DM' : 'Group'}</b> {c.name || ''}<br/>
                  <small>{c.id}</small>
                </div>
              ))}
            </div>
            <hr/>
            <div>
              <h4>New DM</h4>
              <input placeholder="other user UUID" value={dmTarget} onChange={e=>setDmTarget(e.target.value)} />
              <button onClick={createDM}>Create</button>
            </div>
            <div style={{ marginTop:12 }}>
              <h4>New Group</h4>
              <input placeholder="group name" value={groupName} onChange={e=>setGroupName(e.target.value)} /><br/>
              <input placeholder="member UUIDs, comma-separated" value={groupMembers} onChange={e=>setGroupMembers(e.target.value)} /><br/>
              <button onClick={createGroup}>Create</button>
            </div>
          </div>
        )}
      </div>
      <div style={{ display:'flex', flexDirection:'column' }}>
        <div style={{ padding:12, borderBottom:'1px solid #ddd' }}>
          {active ? (<b>Chat: {active.name || active.id} ({active.type})</b>) : <i>Select a chat</i>}
        </div>
        <div style={{ flex:1, padding:12, overflow:'auto' }}>
          {messages.map(m => (
            <div key={m.id} style={{ margin:'6px 0' }}><b>{m.senderName}:</b> {m.content}</div>
          ))}
        </div>
        <div style={{ padding:12, borderTop:'1px solid #ddd', display:'flex', gap:8 }}>
          <input style={{ flex:1 }} placeholder="Type a message..." value={newMsg} onChange={e=>setNewMsg(e.target.value)} onKeyDown={e=>e.key==='Enter' && send()} />
          <button onClick={send}>Send</button>
        </div>
      </div>
    </div>
  )
}

function AuthPanel({ onLogin, onRegister }:{ onLogin:(e:string,p:string)=>void, onRegister:(e:string,p:string,d:string)=>void }){
  const [email,setEmail]=useState('vinay@example.com'); const [password,setPassword]=useState('vinay123'); const [displayName,setDisplayName]=useState('Vinay')
  return (
    <div style={{ display:'grid', gap:6 }}>
      <h4>Register</h4>
      <input value={email} onChange={e=>setEmail(e.target.value)} placeholder="email" />
      <input value={password} onChange={e=>setPassword(e.target.value)} placeholder="password" type="password" />
      <input value={displayName} onChange={e=>setDisplayName(e.target.value)} placeholder="display name" />
      <button onClick={()=>onRegister(email,password,displayName)}>Register</button>
      <h4>Login</h4>
      <button onClick={()=>onLogin(email,password)}>Login</button>
    </div>
  )
}
