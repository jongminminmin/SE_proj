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

- content
  - id(사용자 id. 외래키 : user의 user_id)
  - title(내용 제목, String)
  - content(내용, String)
  - timestamp(기본값 : 시스템 시간(현재시간))
- chat 테이블
  - **작성필요**
---

현재 발생한 문제 상황(2025.03.25, 김종민)
-

- 연구실 컴퓨터의 공인 IP로 접속하는 트래픽을 차단하는 것 같은 문제가 있음.

    그래서 공인 IP -> 사설 IP -> WSL의 MariaDB서버로 트래픽을 전달 할 수 없음
- 대안으로는 tailscale이라는 프로그램 사용으로 팀 내에서 사설 서버를 만들고, 그 IP를 통해 MariaDB 서버에 접속하는 방법이 있음.
---
해결 완료.(2025.03.26, 김종민)
-

- 위에 기술 했다 시피, 연구실 컴퓨터가 이더넷으로 네트워크 환경을 구성하여 인바운드 규칙을 적용해도 공인 IP주소를 통해 들어오는 패킷을 클라이언트로 보내지 못하는 문제가 있었음
- 무선 랜을 사용하는 컴퓨터로 서버를 설정 후, 포트포워딩으로 해당 문제 해결.
- 데이터베이스 작성 가능.


**bash에서**

``sudo mysql -h 61.253.140.23 -P 3306 -u public_test_user -p projdata``


또는

``sudo mysql -h 61.253.140.23 -P 3306 -u root -p projdata``

로 접근 시 접근이 정상적으로 가능함.

Intelij IDE 사용시 어플리케이션 프로퍼티에 작성해놓은 드라이버를 바탕으로 접속 하면 됨.

호스트 : 61.253.140.23

사용자 :root 또는 public_test_user

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