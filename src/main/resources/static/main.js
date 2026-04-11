// ============================================================
// 메인 페이지 프론트엔드 스크립트
// 파일 경로: src/main/resources/static/main.js
// [담당자 작성 필요] 아래 TODO 항목을 채워주세요
// ============================================================


// -------------------------------------------------------
// 페이지가 열리자마자 자동 실행 - 회원 데이터 불러오기
// -------------------------------------------------------
window.onload = async function () {
    await loadUserProfile();
};


// -------------------------------------------------------
// 백엔드에서 회원 개인 데이터를 불러오는 함수
// GET /api/user/profile 호출 → 응답 데이터를 화면에 표시
// -------------------------------------------------------
async function loadUserProfile() {
    const response = await fetch('/api/user/profile');

    // 로그인 안 된 상태면 로그인 페이지로 강제 이동
    if (response.status === 401) {
        window.location.href = '/login';
        return;
    }

    const data = await response.json();
    displayUserProfile(data);
}


// -------------------------------------------------------
// [TODO] 받아온 데이터를 화면 각 영역에 채워 넣는 함수
// data 안에 들어있는 값:
//   data.username   → 이메일 (로그인 아이디)
//   data.name       → 이름
// -------------------------------------------------------
function displayUserProfile(data) {
    // [TODO] 각 HTML 요소에 데이터를 넣는 코드를 작성하세요       ← 45번째 줄
    //
    // 예시:
    // document.getElementById('username').innerText = data.username;
    // document.getElementById('name').innerText     = data.name;
}
