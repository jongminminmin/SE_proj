### 소프트웨어 공학을 위한 프로젝트
* 웹 서비스 구현

- 사용자 관리 로직
    회원 가입, 로그인(소셜로그인 기능 구현)
    사용자 역할(관리자, 일반 사용자)
    비밀번호 보안(최소 8자리 이상, 특수문자 허용)
      -> 이건 프론트 단에서 비밀번호 입력 시 보안을 위한 charactor 설정. 백으로 던지는 value값은 원래 값이어야 함.

- 프로젝트 관리 페이지

- 업무 관리 페이지

- 댓글 및 활동 로그 페이지

- 알림 시스템


figma에서 제공하는 기본 페이지 디자인을 바탕으로 색상 및 기능 구현


---
## 모든 업데이트 사항은 commit 후 push(푸시는 업데이트 사항을 깃허브 리포지토리에 업데이트)
## pull로 로컬에 가져 온 후 업데이트. 그리고 push(pull은 업데이트 된 내용을 로컬에 가져오는 명령어)

**git bash에서**
    
**업데이트 사항 커밋 : git commit -m "업데이트 내용" <branch 코드>**
    
**풀 명령어 : git pull origin master**
    
**푸시 명령어 : git push origin master**

--- 

# 데이터베이스 구조
- user table
  - user_id (String)
  - username(닉네임, String)
  - password(비밀번호, String)
  - email(이메일, String)
  - role ()

- content
  - id(사용자 id. 외래키 : user의 user_id)
  - title(내용 제목, String)
  - content(내용, String)
  - timestamp(기본값 : 시스템 시간(현재시간))
- chat 테이블
  
- user_id varchar(25) foreign key references user(id)
- content varchar(255)
- username varchar(255) unique
- timestamp timestamp -> current_timestamp()
- room_id int autoincrement primary key


**bash에서**

``sudo mysql -h 61.253.140.23 -P 3306 -u public_test_user -p projdata``


또는

``sudo mysql -h 61.253.140.23 -P 3306 -u admin -p projdata``

로 접근 시 접근이 정상적으로 가능함.

Intelij IDE 사용시 어플리케이션 프로퍼티에 작성해놓은 드라이버를 바탕으로 접속 하면 됨.

호스트 : 61.253.140.23

사용자 : **admin** 또는 **public_test_user**

비밀번호는 둘 다 1234로 동일.


데이터베이스는 projdata 사용.



--- 
***데이터베이스 쿼리문***
CREATE TABLE users (
id VARCHAR(255) PRIMARY KEY NOT NULL UNIQUE,
password VARCHAR(50) NOT NULL,
username VARCHAR(36) NOT NULL UNIQUE,
email VARCHAR(50)
);
ALTER TABLE users ADD COLUMN role VARCHAR(20);
ALTER TABLE users ADD COLUMN password_reset_token VARCHAR(255);
ALTER TABLE users ADD COLUMN password_reset_token_expiry DATETIME;
ALTER TABLE users ADD COLUMN project_id INT;

CREATE TABLE projects (
project_id INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
project_title VARCHAR(100),
description VARCHAR(1000),
owner_id VARCHAR(255) NOT NULL,
date DATE DEFAULT (CURRENT_DATE),
project_member_tier VARCHAR(5) NOT NULL,
CONSTRAINT FK_project_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE project_members (
project_id INT NOT NULL,
user_id VARCHAR(255) NOT NULL,
PRIMARY KEY (project_id, user_id),
CONSTRAINT FK_project_members_project FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE,
CONSTRAINT FK_project_members_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE chat_rooms (
id VARCHAR(255) PRIMARY KEY NOT NULL,
int_id INT UNIQUE NOT NULL,
name VARCHAR(255),
description VARCHAR(255),
type VARCHAR(50),
last_message VARCHAR(255),
last_message_time DATETIME DEFAULT CURRENT_TIMESTAMP,
color VARCHAR(20)
);

CREATE TABLE chat_messages (
message_id VARCHAR(255) PRIMARY KEY NOT NULL,
chat_room_id VARCHAR(255) NOT NULL,
sender_id VARCHAR(255) NOT NULL,
username VARCHAR(255) NOT NULL,
content VARCHAR(1000) NOT NULL,
timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT FK_chat_messages_chatroom FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id) ON DELETE CASCADE,
CONSTRAINT FK_chat_messages_sender FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE user_chat_rooms (
id VARCHAR(255) PRIMARY KEY NOT NULL,
user_id VARCHAR(255) NOT NULL,
chat_room_id VARCHAR(255) NOT NULL,
unread_count INT DEFAULT 0,
last_read_message_id VARCHAR(255),
UNIQUE (user_id, chat_room_id),
CONSTRAINT FK_user_chat_rooms_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
CONSTRAINT FK_user_chat_rooms_chatroom FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id) ON DELETE CASCADE,
CONSTRAINT FK_user_chat_rooms_last_read_message FOREIGN KEY (last_read_message_id) REFERENCES chat_messages(message_id) ON DELETE SET NULL
);

CREATE TABLE tasks (
task_no INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
project_id INT,
assignee_id VARCHAR(255),
task_title VARCHAR(100) NOT NULL,
description VARCHAR(1000),
due_start DATE DEFAULT (CURRENT_DATE),
due_end DATE,
task_content LONGTEXT,
created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                       CONSTRAINT FK_task_project FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE SET NULL,
                       CONSTRAINT FK_task_assignee FOREIGN KEY (assignee_id) REFERENCES users(id) ON DELETE SET NULL
);

COMMIT;

