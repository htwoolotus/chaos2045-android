from fastapi import FastAPI, HTTPException,status, Depends, Request
from fastapi.middleware.cors import CORSMiddleware
from firebase_admin import credentials, auth, initialize_app
import os
from typing import Optional, Dict
from pydantic import BaseModel
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from datetime import datetime, timedelta


# Initialize Firebase Admin SDK
# Note: You need to download your Firebase service account key JSON file
# and place it in the same directory as this file
cred = credentials.Certificate("chaos2045-f199c-firebase-adminsdk-fbsvc-19a9887d1e.json")
firebase_app = initialize_app(cred)


app = FastAPI()

# CORS 配置
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 生产环境应更严格
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 初始化 Firebase
# cred = credentials.Certificate("chaos2045-f199c-firebase-adminsdk-fbsvc-19a9887d1e.json")
# firebase = firebase_admin.initialize_app(cred)

# 数据模型
class GoogleAuthRequest(BaseModel):
    idToken: str

class TokenResponse(BaseModel):
    access_token: str
    token_type: str
    uid: str
    email: Optional[str] = None

# 安全方案
security = HTTPBearer()

# 用户会话存储
class SessionInfo:
    def __init__(self, uid: str, email: str, access_token: str, created_at: datetime):
        self.uid = uid
        self.email = email
        self.access_token = access_token
        self.created_at = created_at
        self.last_activity = created_at

active_sessions: Dict[str, SessionInfo] = {}

# 清理过期会话的函数
def cleanup_expired_sessions():
    current_time = datetime.now()
    expired_sessions = [
        uid for uid, session in active_sessions.items()
        if current_time - session.last_activity > timedelta(hours=24)
    ]
    for uid in expired_sessions:
        del active_sessions[uid]

async def verify_token(credentials: HTTPAuthorizationCredentials = Depends(security)):
    try:
        token = credentials.credentials
        print(f"Received token: {token[:20]}...")  # Print first 20 chars of token
        decoded_token = auth.verify_id_token(token)
        print(f"Token verified successfully for user: {decoded_token.get('uid')}")
        
        # 更新会话活动时间
        uid = decoded_token['uid']
        if uid in active_sessions:
            active_sessions[uid].last_activity = datetime.now()
        
        return decoded_token
    except Exception as e:
        print(f"Token verification failed: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="无效的认证凭证",
            headers={"WWW-Authenticate": "Bearer"},
        )

@app.get("/protected")
async def protected_route(decoded_token: dict = Depends(verify_token)):
    return {
        "message": "This is a protected route",
        "user": {
            "uid": decoded_token["uid"],
            "email": decoded_token.get("email")
        }
    }

@app.post("/api/auth/google", response_model=TokenResponse)
async def google_auth(request: GoogleAuthRequest):
    
        print(f"Verifying token(api/auth/google): {request.idToken}...")  # 打印token前20个字符用于调试
        decoded_token = auth.verify_id_token(request.idToken)
        uid = decoded_token['uid']
        email = decoded_token.get('email')
        
        # 生成自定义 token    
        custom_token = auth.create_custom_token(uid)
        access_token = custom_token.decode('utf-8')

        # 创建新会话
        current_time = datetime.now()
        session_info = SessionInfo(
            uid=uid,
            email=email,
            access_token=access_token,
            created_at=current_time
        )
        
        # 保存会话
        active_sessions[uid] = session_info
        
        # 清理过期会话
        cleanup_expired_sessions()
        
        return {
            "access_token": access_token,
            "token_type": "bearer",
            "uid": uid,
            "email": email
        }


async def get_current_user(token: str = Depends(security)):
    try:
        print(f"Verifying token: {token.accessToken}...")  # 打印token前20个字符用于调试
        decoded_token = auth.verify_id_token(token.accessToken)
        return decoded_token
    except Exception as e:
        raise HTTPException(
            status_code=401,
            detail="无效的认证令牌",
            headers={"WWW-Authenticate": "Bearer"},
        )

@app.post("/api/logout")
async def logout_user(current_user: dict = Depends(get_current_user)):
    """
    用户登出端点
    1. 撤销Firebase会话
    2. 清除服务器端会话记录
    """
    uid = current_user.get('uid')
    
    # 1. 撤销Firebase令牌
    try:
        auth.revoke_refresh_tokens(uid)
        print(f"已撤销用户 {uid} 的刷新令牌")
    except Exception as e:
        print(f"撤销令牌失败: {str(e)}")
    
    # 2. 清除服务器会话记录
    if uid in active_sessions:
        del active_sessions[uid]
    
    return {"message": "登出成功", "success": True}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000) 