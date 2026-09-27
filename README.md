# Spring 으로 회원·게시판·댓글 API 만들기

Spring Boot + Spring Security 로 회원 가입·로그인, 게시글, 댓글을 갖춘 게시판 REST API 서버 구축



## 1. 실행 방법

### 환경

| 항목 | 버전 |
|---|---|
| JDK | 21 |
| Spring Boot | 3.5.16 |
| Spring Security | 6.5 |
| DB | H2  |


### 실행 명령

```bash
./gradlew bootRun
```

서버는 `http://localhost:8080`에서 실행됩니다. 

### DB

H2를 파일 모드로 사용하며, 첫 실행 시 프로젝트 루트에 `db_dev.mv.db` 파일이 자동 생성됩니다. 이 파일은 `.gitignore`로 저장소에서 제외했습니다.

```yaml
# src/main/resources/application-dev.yaml
spring:
  datasource:
    url: jdbc:h2:./db_dev;MODE=MySQL
    username: sa
    password:
    driver-class-name: org.h2.Driver
```

- H2 콘솔: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:./db_dev`, User `sa`, Password 없음)
- 처음 상태로 되돌리려면 서버를 종료한 뒤 `rm db_dev*` 후 다시 실행합니다.

### JWT 서명 키

서명 키는 설정 파일에 두지 않고 환경변수 `JWT_SECRET`으로 받습니다.

- **환경변수가 없으면**: 실행할 때마다 임의의 키를 생성합니다. 별도 설정 없이 바로 실행할 수 있지만, 서버를 재시작하면 이전에 발급한 토큰은 무효가 됩니다.
- **고정 키를 쓰려면**: 아래처럼 256비트 이상의 Base64 키를 지정합니다.

```bash
export JWT_SECRET=$(openssl rand -base64 32)
./gradlew bootRun
```

### 테스트 데이터

`dev` 프로필(기본값)로 실행하면, 게시글이 하나도 없을 때 테스트 데이터를 자동 생성합니다. 이미 게시글이 있으면 생성하지 않으므로 재실행해도 중복되지 않습니다.

| 이메일 | 비밀번호 | 닉네임 |
|---|---|---|
| user1@test.com | password123 | 유저1 |
| user2@test.com | password123 | 유저2 |
| user3@test.com | password123 | 유저3 |

- 게시글 12개 (홀수 번째는 유저1, 짝수 번째는 유저2가 작성)
- 댓글 4개 (가장 최근 글에 3개, 그 이전 글에 1개)

---

## 2. API 명세

### 공통

**인증**: 로그인 응답으로 받은 토큰을 요청 헤더에 담습니다.

```
Authorization: Bearer {accessToken}
```

**성공 응답**: 별도의 래퍼 없이 DTO를 그대로 반환합니다. 결과는 HTTP 상태 코드로 구분합니다.

| 상황 | 상태 코드 | 본문 |
|---|---|---|
| 조회·수정·로그인 성공 | 200 OK | 결과 DTO |
| 생성 성공 | 201 Created | 생성된 리소스 DTO |
| 삭제 성공 | 204 No Content | 없음 |

**오류 응답**: 모든 오류는 같은 모양으로 응답합니다.

```json
{
  "status": 400,
  "code": "INVALID_INPUT",
  "message": "입력값이 올바르지 않습니다.",
  "errors": [
    { "field": "email", "reason": "이메일 형식이 올바르지 않습니다." }
  ]
}
```

| 필드 | 설명 |
|---|---|
| `status` | HTTP 상태 코드 |
| `code` | 오류 종류 (아래 표) |
| `message` | 사람이 읽을 수 있는 설명 |
| `errors` | 입력 검증 실패 시 필드별 사유. 그 외에는 빈 배열 |

| code | 상태 | 발생 상황 |
|---|---|---|
| `INVALID_INPUT` | 400 | 검증 실패, JSON 형식 오류, 숫자가 아닌 id 등 |
| `UNAUTHORIZED` | 401 | 로그인이 필요한 요청에 토큰이 없거나 만료·위조된 토큰 |
| `LOGIN_FAILED` | 401 | 이메일 또는 비밀번호 불일치 |
| `FORBIDDEN` | 403 | 남의 글·댓글 수정·삭제 |
| `MEMBER_NOT_FOUND` | 404 | 회원 없음 |
| `POST_NOT_FOUND` | 404 | 게시글 없음 |
| `COMMENT_NOT_FOUND` | 404 | 댓글 없음, 또는 주소의 글에 달린 댓글이 아님 |
| `RESOURCE_NOT_FOUND` | 404 | 없는 주소 |
| `METHOD_NOT_ALLOWED` | 405 | 지원하지 않는 HTTP 메서드 |
| `DUPLICATE_EMAIL` | 409 | 이미 가입된 이메일 |
| `INTERNAL_ERROR` | 500 | 예상하지 못한 서버 오류 (발생하지 않도록 처리) |

### 엔드포인트 목록

| 기능 | 메서드 | 주소 | 인증 | 성공 |
|---|---|---|---|---|
| 회원 가입 | POST | `/api/members` | X | 201 |
| 로그인 | POST | `/api/auth/login` | X | 200 |
| 게시글 쓰기 | POST | `/api/posts` | O | 201 |
| 게시글 목록 | GET | `/api/posts?page={page}&size={size}` | X | 200 |
| 게시글 상세 | GET | `/api/posts/{postId}` | X | 200 |
| 게시글 수정 | PUT | `/api/posts/{postId}` | O (작성자) | 200 |
| 게시글 삭제 | DELETE | `/api/posts/{postId}` | O (작성자) | 204 |
| 댓글 쓰기 | POST | `/api/posts/{postId}/comments` | O | 201 |
| 댓글 목록 | GET | `/api/posts/{postId}/comments` | X | 200 |
| 댓글 수정 | PUT | `/api/posts/{postId}/comments/{commentId}` | O (작성자) | 200 |
| 댓글 삭제 | DELETE | `/api/posts/{postId}/comments/{commentId}` | O (작성자) | 204 |

---

### 회원 가입

`POST /api/members` · 인증 불필요

**요청**

```json
{
  "email": "user@test.com",
  "password": "password123",
  "nickname": "닉네임"
}
```

| 필드 | 규칙 |
|---|---|
| `email` | 필수, 이메일 형식, 100자 이하 |
| `password` | 필수, 8자 이상 64자 이하 |
| `nickname` | 필수, 30자 이하 |

**응답** `201 Created`

```json
{
  "id": 1,
  "email": "user@test.com",
  "nickname": "닉네임",
  "createdAt": "2026-09-27T10:00:00.000000"
}
```

비밀번호는 응답에 포함되지 않습니다.

**오류**: `400` 이메일 형식 오류·짧은 비밀번호·빈 값 · `409` 중복 이메일

---

### 로그인

`POST /api/auth/login` · 인증 불필요

**요청**

```json
{
  "email": "user@test.com",
  "password": "password123"
}
```

**응답** `200 OK`

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer"
}
```

토큰 유효 시간은 1시간입니다.

**오류**: `400` 빈 값 · `401` 이메일 또는 비밀번호 불일치

없는 이메일과 틀린 비밀번호는 같은 `LOGIN_FAILED`로 응답합니다. 둘을 구분하면 특정 이메일의 가입 여부가 노출되기 때문입니다.

---

### 게시글 쓰기

`POST /api/posts` · 인증 필요

**요청**

```json
{
  "title": "제목",
  "content": "본문"
}
```

| 필드 | 규칙 |
|---|---|
| `title` | 필수, 200자 이하 |
| `content` | 필수 |

**응답** `201 Created`

```json
{
  "id": 1,
  "title": "제목",
  "content": "본문",
  "authorId": 1,
  "authorNickname": "닉네임",
  "createdAt": "2026-09-27T10:00:00.000000",
  "updatedAt": "2026-09-27T10:00:00.000000"
}
```

**오류**: `400` 검증 실패 · `401` 비로그인

---

### 게시글 목록

`GET /api/posts?page=0&size=10` · 인증 불필요

| 파라미터 | 기본값 | 설명 |
|---|---|---|
| `page` | 0 | 0부터 시작. 음수는 0으로 보정 |
| `size` | 10 | 1~50. 범위를 벗어나면 보정 |

최신순(id 내림차순)으로 정렬합니다. 목록에는 본문 대신 작성자 닉네임과 댓글 수를 담습니다.

**응답** `200 OK`

```json
{
  "content": [
    {
      "id": 12,
      "title": "테스트 글 12",
      "authorNickname": "유저2",
      "commentCount": 3,
      "createdAt": "2026-09-27T10:00:00.000000"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 12,
  "totalPages": 2,
  "hasNext": true
}
```

**오류**: `400` 숫자가 아닌 `page`·`size`

---

### 게시글 상세

`GET /api/posts/{postId}` · 인증 불필요

**응답** `200 OK`: 게시글 쓰기의 응답과 같은 모양

**오류**: `400` 숫자가 아닌 id · `404` 없는 게시글

---

### 게시글 수정

`PUT /api/posts/{postId}` · 인증 필요 (작성자만)

**요청**: 게시글 쓰기와 같음 (제목, 본문 모두 필수)

**응답** `200 OK`: 수정된 게시글 (`updatedAt` 갱신)

**오류**: `400` 검증 실패 · `401` 비로그인 · `403` 작성자가 아님 · `404` 없는 게시글

---

### 게시글 삭제

`DELETE /api/posts/{postId}` · 인증 필요 (작성자만)

**응답** `204 No Content`

게시글에 달린 댓글도 함께 삭제됩니다.

**오류**: `401` 비로그인 · `403` 작성자가 아님 · `404` 없는 게시글

---

### 댓글 쓰기

`POST /api/posts/{postId}/comments` · 인증 필요

**요청**

```json
{
  "content": "댓글 내용"
}
```

| 필드 | 규칙 |
|---|---|
| `content` | 필수, 1000자 이하 |

**응답** `201 Created`

```json
{
  "id": 1,
  "postId": 12,
  "content": "댓글 내용",
  "authorId": 1,
  "authorNickname": "닉네임",
  "createdAt": "2026-09-27T10:00:00.000000",
  "updatedAt": "2026-09-27T10:00:00.000000"
}
```

**오류**: `400` 검증 실패 · `401` 비로그인 · `404` 없는 게시글

---

### 댓글 목록

`GET /api/posts/{postId}/comments` · 인증 불필요

작성 순서(오래된 순)로 전체를 반환합니다. 한 글에 달리는 댓글 수는 제한적이어서 페이지를 나누지 않았습니다.

**응답** `200 OK`: 댓글 쓰기 응답 모양의 배열. 댓글이 없으면 `[]`

**오류**: `404` 없는 게시글 (댓글이 없는 글은 `[]`, 없는 글은 404로 구분)

---

### 댓글 수정

`PUT /api/posts/{postId}/comments/{commentId}` · 인증 필요 (작성자만)

**요청**: 댓글 쓰기와 같음

**응답** `200 OK`: 수정된 댓글 (`updatedAt` 갱신)

**오류**: `400` 검증 실패 · `401` 비로그인 · `403` 작성자가 아님 · `404` 없는 댓글 또는 해당 글의 댓글이 아님

---

### 댓글 삭제

`DELETE /api/posts/{postId}/comments/{commentId}` · 인증 필요 (작성자만)

**응답** `204 No Content`

**오류**: `401` 비로그인 · `403` 작성자가 아님 · `404` 없는 댓글 또는 해당 글의 댓글이 아님

---

## 3. 설계 설명

### 3-1. 로그인 방식: JWT

세션 대신 JWT(HS256 서명, 유효 시간 1시간)를 선택했습니다.

- **서버가 로그인 상태를 저장하지 않습니다.** 토큰에 회원 ID와 만료 시각이 서명되어 있어, 서버는 서명만 검증하면 됩니다. 세션 저장소가 필요 없어 서버를 여러 대로 늘려도 세션 공유 문제가 없습니다.
- **REST API 클라이언트와 잘 맞습니다.** 웹, 모바일, 다른 서버 어디서든 `Authorization` 헤더 하나로 인증하며, 쿠키에 의존하지 않으므로 CSRF 방어가 필요 없습니다.
- **단점과 대응**: 발급된 토큰은 만료 전까지 서버에서 강제로 무효화할 수 없습니다. 탈취 시 피해 기간을 줄이기 위해 유효 시간을 1시간으로 짧게 두었고, 로그아웃은 클라이언트가 토큰을 폐기하는 방식으로 처리합니다.

**인증 흐름**

```
로그인 → 비밀번호 확인(BCrypt) → JwtProvider가 토큰 발급 (subject = 회원 ID)
   ↓
요청 헤더 Authorization: Bearer {token}
   ↓
JwtAuthenticationFilter: 토큰 검증 → 유효하면 SecurityContext에 회원 ID 저장
   ↓
SecurityConfig 규칙: 인증이 필요한 주소인데 인증 정보가 없으면 401
   ↓
컨트롤러: @AuthenticationPrincipal Long memberId 로 회원 ID 사용
```

- 필터는 토큰이 없거나 잘못되어도 요청을 막지 않고 인증 정보만 채우지 않습니다. 로그인 없이 되는 읽기 요청이 있기 때문에, 접근 허용 판단은 SecurityConfig 한 곳에 모았습니다.
- **401**은 필터 단계에서 `AuthenticationEntryPoint`가 처리합니다.
- **403**(남의 글·댓글)은 게시글을 조회해 작성자를 비교해야 알 수 있으므로, 서비스에서 `BusinessException(FORBIDDEN)`을 던지고 `GlobalExceptionHandler`가 처리합니다. 두 경로 모두 같은 `ErrorResponse` 모양으로 응답합니다.

**접근 규칙**

| 대상 | 규칙 |
|---|---|
| `POST /api/members`, `POST /api/auth/login` | 누구나 |
| `GET /api/posts/**` (글 목록·상세, 댓글 목록) | 누구나 |
| 그 외 모든 요청 | 로그인 필요 |

**비밀번호 저장**: `BCryptPasswordEncoder`로 해시한 값만 저장합니다. BCrypt는 매번 다른 salt를 섞어 같은 비밀번호도 저장 결과가 달라지며, 계산 비용이 커서 무작위 대입 공격에 강합니다. 비밀번호는 어떤 응답에도 포함하지 않습니다.

### 3-2. 엔티티 관계

```
Member 1 ──< N Post
Member 1 ──< N Comment
Post   1 ──< N Comment
```

| 엔티티 | 필드 |
|---|---|
| `Member` | id, email(unique), password(BCrypt 해시), nickname, createdAt, updatedAt |
| `Post` | id, title, content, author(→Member), createdAt, updatedAt |
| `Comment` | id, content, author(→Member), post(→Post), createdAt, updatedAt |

- **공통 필드**: `id`, `createdAt`, `updatedAt`은 `BaseEntity`(`@MappedSuperclass`)에 두고 JPA Auditing으로 자동 기록합니다. 회원의 가입 시각은 `createdAt`입니다.
- **모든 연관관계는 `@ManyToOne(fetch = LAZY)`**: 기본값인 EAGER는 엔티티를 조회할 때마다 연관 엔티티를 추가로 조회해 N+1의 원인이 됩니다. 필요한 곳에서만 fetch join이나 집계 쿼리로 함께 가져옵니다.
- **`Post → Comment` 양방향 컬렉션은 두지 않았습니다.** 댓글 수는 집계 쿼리로, 댓글 목록은 별도 API로, 글 삭제 시 댓글은 벌크 삭제로 처리하므로 컬렉션이 필요 없습니다. 단방향으로 두어 연관관계 관리 코드를 줄이고, 쓰지 않는 컬렉션이 로딩되는 일을 막았습니다.
- **setter 없이 의미 있는 메서드만 공개**: 값 변경은 `update()`로만, 작성자 확인은 `isWrittenBy(memberId)`로 합니다. `isWrittenBy()`는 LAZY 프록시의 `getId()`를 쓰므로 작성자 확인을 위해 추가 쿼리가 발생하지 않습니다.
- **요청과 응답에는 엔티티 대신 DTO**(`record`)를 사용합니다. 엔티티를 그대로 내보내면 비밀번호가 노출되고, 지연 로딩 필드 직렬화 문제가 생깁니다.

### 3-3. 게시글 목록의 N+1 방지

**문제**

목록 응답에는 글마다 작성자 닉네임과 댓글 수가 필요합니다. 글 엔티티를 조회한 뒤 작성자와 댓글 수를 꺼내면, 글 수만큼 쿼리가 추가로 실행됩니다.

```java
Page<Post> posts = postRepository.findAll(pageable);          // 1번
posts.map(p -> p.getAuthor().getNickname());                  // 글마다 회원 조회 → +N번
posts.map(p -> commentRepository.countByPostId(p.getId()));   // 글마다 댓글 수 조회 → +N번
```

`LAZY`는 조회 시점을 늦출 뿐 횟수를 줄이지 않으므로, 목록처럼 모든 글의 작성자가 필요한 경우에는 N+1을 막지 못합니다. 댓글 수는 연관관계가 아닌 집계라서 fetch 전략과 무관하게 글마다 조회됩니다.

**해결: 작성자 조인 + 댓글 수 집계를 한 쿼리로 DTO 조회**

```java
@Query(value = """
        select new com.mission.domain.post.dto.PostListItem(
            p.id, p.title, m.nickname, count(c.id), p.createdAt)
        from Post p
        join p.author m
        left join Comment c on c.post = p
        group by p.id, p.title, m.nickname, p.createdAt
        order by p.id desc
        """,
        countQuery = "select count(p) from Post p")
Page<PostListItem> findPostList(Pageable pageable);
```

- `join p.author m`: 작성자를 같은 쿼리에서 조인해 닉네임을 가져옵니다.
- `left join Comment c on c.post = p` + `count(c.id)` + `group by`: 글별 댓글 수를 집계합니다. `left join`이므로 댓글이 없는 글도 포함되며, 이때 댓글 수는 0입니다.
- `select new ...PostListItem(...)`: 엔티티가 아닌 DTO로 바로 조회하므로 지연 로딩이 발생할 여지가 없습니다.
- `countQuery`: 페이지 정보용 전체 개수를 `group by` 없는 단순 쿼리로 구합니다.
- `order by p.id desc`: IDENTITY 전략이라 id가 클수록 최근 글입니다. 기본 키 인덱스를 사용하고, 같은 시각에 작성된 글의 순서가 섞이지 않습니다.

글 수와 관계없이 목록 조회 쿼리는 **목록 1번 + 전체 개수 1번**으로 고정됩니다.

게시글 상세와 댓글 목록도 `join fetch`로 작성자를 함께 조회해, 작성자 닉네임 때문에 쿼리가 추가되지 않도록 했습니다.

**확인 결과**

게시글 5개(작성자 2명)가 있는 상태에서 목록을 조회했습니다.

① `GET /api/posts?page=0&size=10`: 글 5개가 한 페이지에 모두 들어오는 경우

```sql
select p1_0.id, p1_0.title, a1_0.nickname, count(c1_0.id), p1_0.created_at
from post p1_0
join member a1_0 on a1_0.id = p1_0.author_id
left join comment c1_0 on c1_0.post_id = p1_0.id
group by p1_0.id, p1_0.title, a1_0.nickname, p1_0.created_at
order by p1_0.id desc
fetch first ? rows only
```

실행된 쿼리는 위의 **1번**뿐이며, 5개 글의 id, 제목, 작성자 닉네임(유저1, 유저2), 댓글 수가 한 번에 조회되었습니다. `member`나 `comment`를 글마다 조회하는 쿼리는 없습니다. 첫 페이지에서 가져온 글 수(5)가 페이지 크기(10)보다 작으면 전체 개수가 확정되므로, Spring Data가 count 쿼리를 생략합니다.

② `GET /api/posts?page=0&size=2`: 전체 글이 페이지 크기보다 많은 경우

```sql
-- 목록 (위와 동일한 쿼리, 2건)
select p1_0.id, p1_0.title, a1_0.nickname, count(c1_0.id), p1_0.created_at
from post p1_0
join member a1_0 on a1_0.id = p1_0.author_id
left join comment c1_0 on c1_0.post_id = p1_0.id
group by p1_0.id, p1_0.title, a1_0.nickname, p1_0.created_at
order by p1_0.id desc
fetch first ? rows only

-- 전체 개수 (결과: 5)
select count(p1_0.id) from post p1_0
```

페이지 정보가 필요한 경우에도 쿼리는 **목록 1번 + 개수 1번, 총 2번**입니다.

### 3-4. 글 삭제 시 댓글 처리: 함께 삭제

댓글은 글 없이 의미가 없으므로, 글을 삭제하면 그 글의 댓글도 함께 삭제합니다.

```java
@Transactional
public void delete(Long memberId, Long postId) {
    Post post = postRepository.findById(postId)
            .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    checkAuthor(post, memberId);

    commentRepository.deleteByPostId(postId);   // 댓글 먼저 일괄 삭제
    postRepository.delete(post);
}
```

```java
@Modifying
@Query("delete from Comment c where c.post.id = :postId")
void deleteByPostId(@Param("postId") Long postId);
```

- **댓글을 먼저 삭제**: 댓글이 `post_id` 외래 키로 글을 참조하므로, 글을 먼저 지우면 외래 키 제약 위반이 발생합니다.
- **`cascade = REMOVE` 대신 벌크 삭제**: `cascade = REMOVE`나 메서드 이름으로 만든 삭제 메서드는 댓글을 모두 조회한 뒤 한 건씩 DELETE를 실행합니다. `@Query`로 작성한 벌크 삭제는 댓글 수와 관계없이 DELETE 한 번으로 처리합니다.
- **하나의 트랜잭션**: 댓글 삭제와 글 삭제가 같은 트랜잭션에서 실행되어, 중간에 실패하면 둘 다 롤백됩니다.

**확인 결과**

댓글 4개가 달린 12번 글을 작성자(유저2)가 삭제했을 때 실행된 쿼리입니다.

```sql
-- 1. 글 조회 (작성자 확인)
select p1_0.id, p1_0.author_id, p1_0.content, p1_0.created_at, p1_0.title, p1_0.updated_at
from post p1_0 where p1_0.id=?

-- 2. 댓글 일괄 삭제 (댓글 4개를 한 번에)
delete from comment c1_0 where c1_0.post_id=?

-- 3. 글 삭제
delete from post where id=?
```

삭제 후 `GET /api/posts/12/comments`는 404(`POST_NOT_FOUND`)를 반환합니다.

### 3-5. API 설계 이유

- **자원 중심 주소**: 주소는 대상(`members`, `posts`, `comments`)을 나타내고, 동작은 HTTP 메서드로 구분합니다.
- **댓글을 게시글 하위 주소에 배치** (`/api/posts/{postId}/comments`): 댓글은 반드시 한 글에 속하므로, 어느 글의 댓글인지가 주소에 드러나게 했습니다. 수정·삭제 시에도 주소의 글과 댓글의 실제 소속 글이 다르면 404로 응답해, 주소와 데이터가 어긋난 요청이 처리되지 않도록 했습니다.
- **로그인은 `/api/auth/login`으로 분리**: 가입은 회원 리소스를 만드는 일이고, 로그인은 자격을 확인해 토큰을 발급하는 일이라 주소와 패키지를 나눴습니다. 로그인은 새 리소스를 만들지 않으므로 201이 아닌 200을 반환합니다.
- **수정은 PATCH 대신 PUT**: 제목과 본문을 모두 필수로 받아 통째로 교체합니다. 일부 필드만 보내는 PATCH는 null 처리와 검증이 복잡해지므로, 필드 수가 적은 이번 API에서는 PUT이 명확합니다.
- **상태 코드로 결과 구분**: 생성은 201, 삭제는 204(본문 없음), 조회·수정은 200입니다. 성공 응답을 별도 래퍼로 감싸지 않고, 오류일 때만 공통 `ErrorResponse`를 사용합니다.
- **401과 403 구분**: "누구인지 모름"(토큰 없음, 만료, 로그인 실패)은 401, "누구인지는 알지만 권한 없음"(남의 글·댓글)은 403입니다.
- **중복 이메일은 409 Conflict**: 입력 형식은 올바르지만 기존 데이터와 충돌하는 경우라 400과 구분했습니다. 
- **500 방지**: 검증 실패, JSON 형식 오류, 경로 변수 타입 오류, 없는 주소, 잘못된 메서드를 모두 `GlobalExceptionHandler`에서 4xx로 변환합니다. 페이지 파라미터는 서비스에서 범위를 보정해 잘못된 값이 예외로 이어지지 않게 했습니다.

---

## 4. 실행 결과

테스트 데이터가 생성된 상태에서 새 회원으로 전체 흐름을 호출했습니다. 토큰은 로그인 응답에서 추출해 변수로 사용했습니다.

### 4-1. 회원 가입

```bash
curl -i -X POST http://localhost:8080/api/members \
  -H "Content-Type: application/json" \
  -d '{"email":"tester@test.com","password":"password123","nickname":"테스터"}'
```

```
HTTP/1.1 201
Content-Type: application/json

{"id":4,"email":"tester@test.com","nickname":"테스터","createdAt":"2026-09-27T11:23:23.329818"}
```

응답에 비밀번호는 포함되지 않습니다.

### 4-2. 로그인

```bash
curl -i -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"tester@test.com","password":"password123"}'
```

```
HTTP/1.1 200
Content-Type: application/json

{"accessToken":"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI0IiwiaWF0IjoxNzkwNDc1ODI3LCJleHAiOjE3OTA0Nzk0Mjd9.5eIm3FSqXe4wbUAPYSJ-PSUNsTAtZGCAmAdlw38ckws","tokenType":"Bearer"}
```

토큰의 내용(가운데 부분)을 Base64로 풀면 회원 ID와 발급·만료 시각이 들어 있으며, 만료는 발급 1시간 뒤입니다.

```
{"sub":"4","iat":1790475887,"exp":1790479487}
```

이후 요청에서 사용할 토큰은 변수에 저장했습니다.

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"tester@test.com","password":"password123"}' \
  | sed 's/.*"accessToken":"\([^"]*\)".*/\1/')
```

### 4-3. 글 쓰기

```bash
curl -i -X POST http://localhost:8080/api/posts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"테스터의 첫 글","content":"안녕하세요, 테스터입니다."}'
```

```
HTTP/1.1 201
Content-Type: application/json

{"id":13,"title":"테스터의 첫 글","content":"안녕하세요, 테스터입니다.","authorId":4,"authorNickname":"테스터","createdAt":"2026-09-27T11:24:53.341852","updatedAt":"2026-09-27T11:24:53.341852"}
```

### 4-4. 댓글 쓰기

```bash
curl -i -X POST http://localhost:8080/api/posts/13/comments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content":"첫 댓글입니다."}'
```

```
HTTP/1.1 201
Content-Type: application/json

{"id":6,"postId":13,"content":"첫 댓글입니다.","authorId":4,"authorNickname":"테스터","createdAt":"2026-09-27T11:27:54.047908","updatedAt":"2026-09-27T11:27:54.047908"}
```

### 4-5. 목록 조회 (로그인 없이)

**게시글 목록**: 방금 쓴 글이 최신순으로 맨 위에 있고, 댓글 수가 1로 집계됩니다.

```bash
curl -i "http://localhost:8080/api/posts?page=0&size=10"
```

```
HTTP/1.1 200
Content-Type: application/json

{
  "content": [
    {"id":13,"title":"테스터의 첫 글","authorNickname":"테스터","commentCount":1,"createdAt":"2026-09-27T11:24:53.341852"},
    {"id":11,"title":"테스트 글 11","authorNickname":"유저1","commentCount":0,"createdAt":"2026-09-27T11:12:26.750447"},
    {"id":10,"title":"테스트 글 10","authorNickname":"유저2","commentCount":0,"createdAt":"2026-09-27T11:12:26.745865"},
    {"id":9,"title":"테스트 글 9","authorNickname":"유저1","commentCount":0,"createdAt":"2026-09-27T11:12:26.741636"},
    {"id":8,"title":"테스트 글 8","authorNickname":"유저2","commentCount":0,"createdAt":"2026-09-27T11:12:26.736545"},
    {"id":7,"title":"테스트 글 7","authorNickname":"유저1","commentCount":0,"createdAt":"2026-09-27T11:12:26.732852"},
    {"id":6,"title":"테스트 글 6","authorNickname":"유저2","commentCount":0,"createdAt":"2026-09-27T11:12:26.728696"},
    {"id":5,"title":"테스트 글 5","authorNickname":"유저1","commentCount":0,"createdAt":"2026-09-27T11:12:26.724616"},
    {"id":4,"title":"테스트 글 4","authorNickname":"유저2","commentCount":0,"createdAt":"2026-09-27T11:12:26.720633"},
    {"id":3,"title":"테스트 글 3","authorNickname":"유저1","commentCount":0,"createdAt":"2026-09-27T11:12:26.716437"}
  ],
  "page": 0,
  "size": 10,
  "totalElements": 12,
  "totalPages": 2,
  "hasNext": true
}
```

**댓글 목록**

```bash
curl -i http://localhost:8080/api/posts/13/comments
```

```
HTTP/1.1 200
Content-Type: application/json

[{"id":6,"postId":13,"content":"첫 댓글입니다.","authorId":4,"authorNickname":"테스터","createdAt":"2026-09-27T11:27:54.047908","updatedAt":"2026-09-27T11:27:54.047908"}]
```

### 4-6. 401: 로그인 없이 글 쓰기

```bash
curl -i -X POST http://localhost:8080/api/posts \
  -H "Content-Type: application/json" \
  -d '{"title":"비로그인 글","content":"본문"}'
```

```
HTTP/1.1 401
Content-Type: application/json;charset=UTF-8

{"status":401,"code":"UNAUTHORIZED","message":"로그인이 필요합니다.","errors":[]}
```

### 4-7. 403: 남의 글 수정

테스트 계정 `user1@test.com`(유저1)으로 로그인해, 테스터가 쓴 13번 글을 수정합니다.

```bash
TOKEN_USER1=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user1@test.com","password":"password123"}' \
  | sed 's/.*"accessToken":"\([^"]*\)".*/\1/')

curl -i -X PUT http://localhost:8080/api/posts/13 \
  -H "Authorization: Bearer $TOKEN_USER1" \
  -H "Content-Type: application/json" \
  -d '{"title":"남의 글 수정 시도","content":"수정"}'
```

```
HTTP/1.1 403
Content-Type: application/json

{"status":403,"code":"FORBIDDEN","message":"권한이 없습니다.","errors":[]}
```

401은 필터 단계(`AuthenticationEntryPoint`)에서, 403은 서비스의 작성자 확인에서 발생하지만 두 응답 모두 같은 오류 모양입니다.