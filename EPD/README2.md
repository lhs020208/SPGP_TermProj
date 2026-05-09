<img width="922" height="602" alt="screenshot" src="https://github.com/user-attachments/assets/ab14cdee-6ba3-4d33-9e3a-0bb95ef49a84" />
# 프로젝트 제목: Element Puzzle Drag

# 게임 소개

> 하나의 드롭을 드래그로 이동시키며 퍼즐 구조를 재배치하고, 연쇄 콤보를 통해 공격을 수행하는 실시간 퍼즐 게임

# 현재까지의 진행 상황

| 항목 | 진행 정도 |        부족 사유 |
|---|------:|-------------:|
| 리소스 확보 |   95% | 연출용 이펙트 등 부족 |
| 기본 퍼즐 화면 구현 |  100% |
| 퍼즐 시스템 구현 (드롭 드래그) |  100% |
| 퍼즐 시스템 구현 (연쇄) |  100% |
| 플레이어 스킬 구현 |  100% |
| 플레이어 공격 구현 |   95% | 공격 이펙트 추가 예정 |
| 몬스터 공격 구현 |   95% | 공격 이펙트 추가 예정 |
| 레벨 밸런싱 |    0% |
| 시작 화면 구현 |    0% |
| 클리어 화면 구현 |    0% |
| 게임 오버 화면 구현 |    0% |
| 게임 루프 구현 (재시작 포함) |    0% |

# git commit

<img width="922" height="602" alt="screenshot" src="https://github.com/user-attachments/assets/87afac85-3d3d-4c85-9a04-52084b89a8bc" />


# 변경 내용

1. 퍼즐 조작 시간을 10초에서 20초로 변경: 실제 조작 해보니 10초는 조작하기에 너무 짧다고 판단.

# Activity 구성

현재 Activity는 `MainActivity`와 `ElementPuzzleDrag` 두 개로 구성되어 있다.

## MainActivity

앱 실행 시 처음 표시되는 Activity이다. 현재는 시작 화면 역할을 담당하며, 시작 버튼을 누르면 실제 게임 화면 Activity인 `ElementPuzzleDrag`를 실행한다.

- 역할
  - 앱의 진입점
  - 시작 화면 표시
  - 게임 시작 버튼 입력 처리
- 핵심 동작
  - `onStartGameClicked()`에서 `startGameActivity()`를 호출한다.
  - `Intent`를 이용해 `ElementPuzzleDrag` Activity로 이동한다.

## ElementPuzzleDrag

실제 게임 루프가 실행되는 Activity이다. `BaseGameActivity`를 상속하며, 게임 화면의 기준 해상도를 900 x 1600으로 설정한 뒤 `MainScene`을 루트 Scene으로 생성한다.

- 역할
  - 게임 화면 실행
  - 게임 Scene 생성
  - 디버그 그리드, 디버그 정보, FPS 그래프 표시
- 핵심 동작
  - `createRootScene()`에서 `MainScene`을 생성한다.
  - 게임 월드의 논리 해상도를 900 x 1600으로 설정한다.

## AndroidManifest 설정

`MainActivity`는 런처 Activity로 등록되어 있고, `ElementPuzzleDrag`는 게임 전용 Activity로 등록되어 있다. `ElementPuzzleDrag`에는 `screenOrientation="nosensor"`가 적용되어 있어 게임 중 스마트폰 회전으로 화면 방향이 바뀌지 않도록 설정되어 있다.

# Scene 구성 및 전환 관계

현재 Scene은 `MainScene` 하나를 중심으로 구성되어 있다.

```text
MainActivity
  └─ 시작 버튼 클릭
      └─ ElementPuzzleDrag Activity
          └─ MainScene

