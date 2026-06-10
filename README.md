<h1 align="center"> 옷장..난감! </h1>

<p align="center">
  <strong>"개인 디지털 옷장 기반 의류 및 코디 추천 서비스"</strong>
  <br />
  사용자의 옷장 데이터와 스타일 취향을 바탕으로 어울리는 상품과 코디를 추천합니다.
</p>

<p align="center">
  Frontend Repository :
  <a href="https://github.com/prgrms-aibe-devcourse/AIBE5_FinalProject_Team4_FE">
    AIBE5_FinalProject_Team4_FE
  </a>
</p>

<h2 align="center"> 🗂️ Project Overview </h2>

<p align="center">
  옷장난감은 사용자가 보유한 옷과 관심 상품을 디지털 옷장에 등록하고,<br />
  <strong>옷장 데이터와 취향 데이터를 기반으로 개인화된 상품과 코디를 추천받을 수 있는 서비스입니다.</strong>
  <br />
  이 저장소는 위 서비스의 API, 도메인 로직, 데이터 저장을 담당합니다.
  <br />
  <br />
  개발 기간 : 2026.05.19 ~ 2026.06.26
</p>

<h2 align="center"> 📖 Documentation </h2>

<p align="center">
  프로젝트의 원활한 협업을 위한 <strong>가이드라인과 공식 문서</strong>입니다.<br />
  작업을 시작하기 전 Wiki와 <strong>docs</strong> 문서를 함께 확인해 주세요!
</p>

<table align="center">
  <tr align="center">
    <td>
      <a href="https://github.com/prgrms-aibe-devcourse/AIBE5_FinalProject_Team4_BE/wiki/BE%E2%80%90GettingStarted">
        <img src="https://img.shields.io/badge/Setup-⚙️-blue?style=for-the-badge" alt="Setup" />
      </a>
    </td>
    <td>
      <a href="https://github.com/prgrms-aibe-devcourse/AIBE5_FinalProject_Team4_BE/wiki/BranchRule">
        <img src="https://img.shields.io/badge/Strategy-🌳-green?style=for-the-badge" alt="Strategy" />
      </a>
    </td>
    <td>
      <a href="https://www.notion.so/35d3550b7b55811a912dca65f0d0fedd#35d3550b7b5581619fdef191421a3252">
        <img src="https://img.shields.io/badge/Convention-✅-orange?style=for-the-badge" alt="Convention" />
      </a>
    </td>
    <td>
      <a href="https://github.com/prgrms-aibe-devcourse/AIBE5_FinalProject_Team4_BE/wiki/Folder%E2%80%90structure">
        <img src="https://img.shields.io/badge/Structure-📂-purple?style=for-the-badge" alt="Structure" />
      </a>
    </td>
    <td>
      <a href="./docs/README.md">
        <img src="https://img.shields.io/badge/Docs-📚-black?style=for-the-badge" alt="Docs" />
      </a>
    </td>
  </tr>
  <tr align="center">
    <td><a href="https://github.com/prgrms-aibe-devcourse/AIBE5_FinalProject_Team4_BE/wiki/BE%E2%80%90GettingStarted"><strong>개발환경 세팅</strong></a></td>
    <td><a href="https://github.com/prgrms-aibe-devcourse/AIBE5_FinalProject_Team4_BE/wiki/BranchRule"><strong>브랜치 전략</strong></a></td>
    <td><a href="https://www.notion.so/35d3550b7b55811a912dca65f0d0fedd#35d3550b7b5581619fdef191421a3252"><strong>팀 컨벤션</strong></a></td>
    <td><a href="https://github.com/prgrms-aibe-devcourse/AIBE5_FinalProject_Team4_BE/wiki/Folder%E2%80%90structure"><strong>폴더 구조</strong></a></td>
    <td><a href="./docs/README.md"><strong>공식 문서</strong></a></td>
  </tr>
</table>

<h2 align="center"> 📚 Stacks </h2>

<table align="center">
  <thead>
    <tr align="center">
      <th>구분</th>
      <th>기술</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>Language</td>
      <td>Java 21</td>
    </tr>
    <tr>
      <td>Framework</td>
      <td>Spring Boot 3.5.14</td>
    </tr>
    <tr>
      <td>Project</td>
      <td>Gradle</td>
    </tr>
    <tr>
      <td>Database</td>
      <td>MySQL 8.4</td>
    </tr>
    <tr>
      <td>Auth</td>
      <td>Spring Security / OAuth2 / JWT</td>
    </tr>
    <tr>
      <td>External</td>
      <td>Naver Shopping API / Gemini API / Weather API</td>
    </tr>
    <tr>
      <td>Deployment</td>
      <td>EC2 / RDS / S3 / Docker Compose</td>
    </tr>
  </tbody>
</table>

<h2 align="center"> 🛠️ Features </h2>

<table align="center">
  <thead>
    <tr align="center">
      <th>기능</th>
      <th>설명</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>소셜 로그인</td>
      <td>카카오, 구글, 네이버 OAuth 로그인을 통해 사용자를 인증합니다.</td>
    </tr>
    <tr>
      <td>디지털 옷장</td>
      <td>사용자가 보유한 옷과 미보유 관심 상품을 하나의 옷장에서 관리합니다.</td>
    </tr>
    <tr>
      <td>옷 등록</td>
      <td>사진 기반 등록, 구매내역 기반 등록, 외부 쇼핑몰 상품 저장을 지원합니다.</td>
    </tr>
    <tr>
      <td>스타일 점수</td>
      <td>온보딩, 옷장 데이터, 추천 피드백을 합산해 사용자 스타일 성향을 관리합니다.</td>
    </tr>
    <tr>
      <td>개인화 추천</td>
      <td>사용자 기반 OOTD, 유사 상품, 어울리는 옷, AI MD 추천을 제공합니다.</td>
    </tr>
    <tr>
      <td>코디북</td>
      <td>추천받거나 직접 구성한 코디를 저장하고 조회합니다.</td>
    </tr>
    <tr>
      <td>룩피드</td>
      <td>코디 기반 게시글, 좋아요, 댓글, 저장, 팔로우 기능을 제공합니다.</td>
    </tr>
  </tbody>
</table>

<h2 align="center"> 🧪 Local Development </h2>

<p align="center">
  로컬 개발 인프라는 Docker Compose로 실행합니다.
</p>

```bash
docker compose up -d
```

<p align="center">
  애플리케이션은 Gradle로 실행합니다.
</p>

```bash
./gradlew bootRun
```

<p align="center">
  Swagger UI는 애플리케이션 실행 후 아래 경로에서 확인합니다.
</p>

```text
http://localhost:8080/swagger-ui.html
```

<h2 align="center"> 🦖 "Team 우주 최강 공룡" 🚀</h2>

<table align="center">
  <tr align="center">
    <td><strong>류태우</strong></td>
    <td><strong>이석민</strong></td>
    <td><strong>김세준</strong></td>
    <td><strong>최준영</strong></td>
    <td><strong>홍가현</strong></td>
  </tr>
  <tr align="center">
    <td>
      <a href="https://github.com/taeaeuu">
        <img src="https://avatars.githubusercontent.com/u/222783261?v=4" width="120px;" alt="류태우" />
      </a>
    </td>
    <td>
      <a href="https://github.com/seokminseok">
        <img src="https://avatars.githubusercontent.com/u/183383691?v=4" width="120px;" alt="이석민" />
      </a>
    </td>
    <td>
      <a href="https://github.com/warcat12">
        <img src="https://avatars.githubusercontent.com/u/252306343?v=4" width="120px;" alt="김세준" />
      </a>
    </td>
    <td>
      <a href="https://github.com/jychoi0831">
        <img src="https://avatars.githubusercontent.com/u/252291780?v=4" width="120px;" alt="최준영" />
      </a>
    </td>
    <td>
      <a href="https://github.com/devken65">
        <img src="https://avatars.githubusercontent.com/u/71168366?v=4" width="120px;" alt="홍가현" />
      </a>
    </td>
  </tr>
  <tr align="center">
    <td><a href="https://github.com/taeaeuu">@taeaeuu</a></td>
    <td><a href="https://github.com/seokminseok">@seokminseok</a></td>
    <td><a href="https://github.com/warcat12">@warcat12</a></td>
    <td><a href="https://github.com/jychoi0831">@jychoi0831</a></td>
    <td><a href="https://github.com/devken65">@devken65</a></td>
  </tr>
  <tr align="center">
    <td>🧠 <b>팀장</b></td>
    <td>👤 <b>팀원</b></td>
    <td>👤 <b>팀원</b></td>
    <td>👤 <b>팀원</b></td>
    <td>👤 <b>팀원</b></td>
  </tr>
</table>
