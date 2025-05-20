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

``sudo mysql -h 61.253.140.23 -P 3306 -u root -p projdata``

로 접근 시 접근이 정상적으로 가능함.

Intelij IDE 사용시 어플리케이션 프로퍼티에 작성해놓은 드라이버를 바탕으로 접속 하면 됨.

호스트 : 61.253.140.23

사용자 : **admin** 또는 **public_test_user**

비밀번호는 둘 다 1234로 동일.


데이터베이스는 projdata 사용.



--- 
***데이터베이스 쿼리문***

CREATE TABLE user(
id varchar(25) NOT NULL UNIQUE PRIMARY KEY ,
password varchar(50) NOT NULL,
username varchar(36) NOT NULL UNIQUE ,
email varchar(50)
);

CREATE TABLE project(
project_id int PRIMARY KEY NOT NULL ,
project_title varchar(100),
description varchar(1000),
owner_id varchar(25) NOT NULL ,
date DATE default SYSDATE(),
project_member_tier varchar(5) NOT NULL ,
constraint foreign key (owner_id) references user(id)
);

CREATE TABLE chat(
user_id varchar(25) NOT NULL,
content varchar(5000),
username varchar(36) NOT NULL UNIQUE ,
timestamp date default SYSDATE(),
room_id int NOT NULL AUTO_INCREMENT PRIMARY KEY ,
constraint fk_user foreign key (user_id) references user(id)
);

CREATE TABLE task(
task_no int PRIMARY KEY NOT NULL ,
project_title varchar(100),
assignee_id varchar(25),
task_title varchar(100) NOT NULL ,
description varchar(1000),
due_start date default SYSDATE(),
due_end date
);

commit;

ALTER TABLE chat
MODIFY COLUMN timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
