# 🧵 사용자 포인트 동시성 제어 설계 보고서

## 📌 개요

이 시스템은 사용자별 포인트 충전 및 사용 기능을 제공하며, 동시에 여러 요청이 들어올 경우 **데이터 정합성**을 유지하기 위해 **Key 기반 동기화 제어**를 도입했습니다.
`SynchronizedLockTemplate`을 통해 사용자 ID 단위로 **synchronized 블록을 안전하게 래핑**하고, 락 충돌/재시도/타임아웃 처리까지 포괄적으로 관리합니다.

## 🎯 동시성 제어의 필요성

- **공유 자원 보호**
  포인트 정보는 여러 스레드에서 동시에 접근 및 수정될 수 있습니다.
- **일관성 보장**
  충전/사용/이력 기록은 원자적 작업으로 처리되어야 합니다.
- **데이터 손실 방지**
  적절한 락 처리가 없다면 Race Condition으로 인해 포인트가 소실되거나 과다 차감될 수 있습니다.

## 🧱 구성 요소

### 1. `SynchronizedLockTemplate<K>`

- Key 기반 락 획득 및 동기화 블록 실행 템플릿
- 타임아웃, 재시도, 인터럽트, 예외 래핑 처리 포함
- `synchronized(lock)`을 안전하게 관리

### 2. `PointLockManager<K>`

- Key별로 락 객체 관리
- `WeakReference`와 `ReferenceQueue`를 활용해 GC 대상이 되면 자동 정리
- 동일한 Key에 대해 동일한 락 객체 보장

### 3. `SynchronizedBlock<T>`

- 실행할 사용자 정의 블록 (`() -> T`)
- 락 획득 이후 실행되는 임계 구역

## 🔄 동작 흐름

```plaintext
UserService
  ├─ chargePoint(userId, amount)
  └─ usePoint(userId, amount)
        │
        ▼
SynchronizedLockTemplate.executeWithLock(userId, block)
        │
        ▼
PointLockManager.getLock(userId)
        │
        ▼
synchronized(lock) {
    block.execute()  ← 포인트 계산 + 저장 + 이력기록
}
```
