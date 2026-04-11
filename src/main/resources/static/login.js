// ============================================================
// 로그인 프론트엔드 처리 스크립트
// 파일 경로: src/main/resources/static/login.js
//
// [사용법]
// 선생님 로그인 페이지에서는 아래 줄을 추가하세요:
//   <script>const USER_TYPE = 'teacher';</script>
// 학생 로그인 페이지에서는:
//   <script>const USER_TYPE = 'student';</script>
// ============================================================


// -------------------------------------------------------
// 로그인 버튼 클릭 시 호출 - 백엔드로 이메일/비밀번호 전송
// -------------------------------------------------------
async function submitLogin() {
    // [TODO] 이메일 입력 필드에서 값을 가져오는 코드를 작성하세요    ← 18번째 줄
    const username = null;

    // [TODO] 비밀번호 입력 필드에서 값을 가져오는 코드를 작성하세요  ← 21번째 줄
    const password = null;

    const response = await fetch('/api/' + USER_TYPE + '/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
    });

    const data = await response.json();
    handleLoginResponse(data, username);
}


// -------------------------------------------------------
// 서버 응답 분기 처리
// data.locked = true  → 로그인 폼 숨기기 + 카운트다운 표시
// data.success = true → 다음 페이지로 이동
// data.success = false → 실패 메시지 + 남은 횟수 표시
// -------------------------------------------------------
function handleLoginResponse(data, username) {
    if (data.locked) {
        hideLoginForm();
        startCountdown(data.remainingSeconds);
        return;
    }
    if (data.success) {
        window.location.href = data.redirectUrl;
        return;
    }
    showErrorMessage(data.message, data.remainingAttempts);
}


// -------------------------------------------------------
// [TODO] 로그인 폼을 화면에서 숨기는 함수
// 5회 실패 또는 잠금 상태일 때 호출됩니다
// -------------------------------------------------------
function hideLoginForm() {
    // [TODO] 로그인 폼 요소를 숨기는 코드를 여기에 작성하세요       ← 59번째 줄
    // 예시: document.getElementById('login-form').style.display = 'none';
}


// -------------------------------------------------------
// [TODO] 잠금 카운트다운을 화면에 표시하는 함수
// remainingSeconds: 잠금 해제까지 남은 초 (서버에서 전달)
// -------------------------------------------------------
function startCountdown(remainingSeconds) {
    // [TODO] 카운트다운 타이머 UI를 여기에 구현하세요               ← 69번째 줄
    // - remainingSeconds를 1초마다 줄여서 화면에 표시하세요
    // - 0이 되면 로그인 폼을 다시 표시하세요 (showLoginForm 호출)
    // 예시:
    // const timer = setInterval(() => {
    //     if (remainingSeconds <= 0) {
    //         clearInterval(timer);
    //         showLoginForm(); // [TODO] 로그인 폼 다시 보여주는 함수 작성 필요
    //         return;
    //     }
    //     document.getElementById('countdown').innerText = remainingSeconds + '초 후 재시도 가능';
    //     remainingSeconds--;
    // }, 1000);
}


// -------------------------------------------------------
// [TODO] 로그인 실패 메시지와 남은 시도 횟수를 표시하는 함수
// -------------------------------------------------------
function showErrorMessage(message, remainingAttempts) {
    // [TODO] 에러 메시지 표시 UI를 여기에 구현하세요                ← 89번째 줄
    // 예시: document.getElementById('error-msg').innerText = message;
}


// -------------------------------------------------------
// [TODO] 페이지 로드 시 잠금 상태를 확인하는 함수
// 사용자가 새로고침해도 잠금이 유지됩니다
// -------------------------------------------------------
async function checkLockoutOnLoad(username) {
    // [TODO] 아래 주석을 해제하고 username을 가져오는 방법을 구현하세요  ← 100번째 줄
    // const response = await fetch('/api/login/status?username=' + username + '&userType=' + USER_TYPE);
    // const data = await response.json();
    // if (data.locked) {
    //     hideLoginForm();
    //     startCountdown(data.remainingSeconds);
    // }
}

// -------------------------------------------------------
// 페이지 로드 시 자동 실행
// [TODO] username을 어떻게 가져올지 구현하세요                      ← 113번째 줄
// -------------------------------------------------------
// window.onload = () => checkLockoutOnLoad(저장된_이메일);
