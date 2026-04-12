-- 사용된 ID 시퀀스 관리 테이블
CREATE TABLE used_id
(
    id            BIGSERIAL PRIMARY KEY,
    type          VARCHAR(10) NOT NULL UNIQUE,
    capacity      INTEGER     NOT NULL DEFAULT 100000,
    current_seq   BIGINT      NOT NULL DEFAULT 0,
    end_seq       BIGINT      NOT NULL DEFAULT 0,
    seq_increment BIGINT      NOT NULL DEFAULT 1,
    seq_range     INTEGER     NOT NULL DEFAULT 0,
    count         BIGINT      NOT NULL DEFAULT 0
);

COMMENT ON TABLE used_id IS 'ID 생성 시퀀스 관리';
COMMENT ON COLUMN used_id.type IS 'ID 타입 접두사';
COMMENT ON COLUMN used_id.capacity IS '범위당 용량 (기본 100,000)';
COMMENT ON COLUMN used_id.current_seq IS '현재 시퀀스 위치';
COMMENT ON COLUMN used_id.end_seq IS '종료 시퀀스 (참조용)';
COMMENT ON COLUMN used_id.seq_increment IS '서로소 증분값';
COMMENT ON COLUMN used_id.seq_range IS '현재 범위 인덱스';
COMMENT ON COLUMN used_id.count IS '현재 범위에서 생성된 ID 수';

-- 사전 생성된 랜덤 ID 값 테이블
CREATE TABLE random_id_generator
(
    id_generation_seq BIGSERIAL PRIMARY KEY,
    random_no         VARCHAR(4)  NOT NULL,
    created_date_time TIMESTAMP   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE random_id_generator IS '사전 생성된 랜덤 ID 값';
COMMENT ON COLUMN random_id_generator.random_no IS 'Base33 랜덤 4자리 값';
COMMENT ON COLUMN random_id_generator.created_date_time IS '생성 시각';

-- 인덱스
CREATE INDEX idx_used_id_type ON used_id (type);
