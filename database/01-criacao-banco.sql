IF DB_ID('SABESP') IS NULL
    CREATE DATABASE SABESP;
GO

USE SABESP;
GO

CREATE TABLE Usuario (
    id_usuario        INT IDENTITY(1,1) NOT NULL,
    nm_usuario        VARCHAR(120)      NOT NULL,
    nm_email          VARCHAR(150)      NOT NULL,
    cd_senha          VARCHAR(255)      NULL,
    cd_telefone       VARCHAR(20)       NOT NULL,
    cd_tipo_usuario   VARCHAR(20)       NOT NULL CONSTRAINT DF_Usuario_Tipo DEFAULT 'CLIENTE',
    ic_estado_usuario BIT               NOT NULL CONSTRAINT DF_Usuario_Estado DEFAULT 1,
    dt_cadastro       DATE              NOT NULL CONSTRAINT DF_Usuario_Cadastro DEFAULT (GETDATE()),

    CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
    CONSTRAINT uq_usuario_email UNIQUE (nm_email),
    CONSTRAINT CK_Usuario_TipoUsuario CHECK (cd_tipo_usuario IN ('ADMIN', 'GERENTE', 'SUPORTE', 'CLIENTE'))
);
GO

CREATE TABLE Localizacao (
    id_localizacao      INT IDENTITY(1,1) NOT NULL,
    cd_cep              CHAR(9)           NOT NULL,
    cd_bairro           VARCHAR(150)      NOT NULL,
    cd_logradouro       VARCHAR(150)      NOT NULL,
    cd_numero           VARCHAR(20)       NULL,
    cd_complemento      VARCHAR(100)      NULL,
    ds_ponto_referencia VARCHAR(255)      NULL,
    cd_latitude         DECIMAL(10,7)     NULL,
    cd_longitude        DECIMAL(10,7)     NULL,
    ic_estado_bairro    BIT               NOT NULL CONSTRAINT DF_Localizacao_Estado DEFAULT 1,

    CONSTRAINT pk_localizacao PRIMARY KEY (id_localizacao),
    CONSTRAINT CK_Localizacao_Cep CHECK (cd_cep LIKE '[0-9][0-9][0-9][0-9][0-9]-[0-9][0-9][0-9]'),
    CONSTRAINT CK_Localizacao_Latitude CHECK (cd_latitude IS NULL OR cd_latitude BETWEEN -90 AND 90),
    CONSTRAINT CK_Localizacao_Longitude CHECK (cd_longitude IS NULL OR cd_longitude BETWEEN -180 AND 180)
);
GO

CREATE TABLE Tipo_ocorrencia (
    id_tipo_ocorrencia INT IDENTITY(1,1) NOT NULL,
    nm_tipo_ocorrencia VARCHAR(100)      NOT NULL,
    ds_tipo_ocorrencia VARCHAR(255)      NULL,
    ic_tipo_ocorrencia BIT               NOT NULL CONSTRAINT DF_TipoOcorrencia_Estado DEFAULT 1,

    CONSTRAINT pk_tipo_ocorrencia PRIMARY KEY (id_tipo_ocorrencia),
    CONSTRAINT uq_tipo_ocorrencia_nome UNIQUE (nm_tipo_ocorrencia)
);
GO

INSERT INTO Tipo_ocorrencia (nm_tipo_ocorrencia) VALUES
    (N'Vazamento de água na rua'),
    (N'Bueiro com vazamento'),
    (N'Falta de água'),
    (N'Esgoto a céu aberto'),
    (N'Entupimento de rede'),
    (N'Outros');
GO

CREATE TABLE Ocorrencia (
    id_ocorrencia       INT IDENTITY(1,1) NOT NULL,
    nm_protocolo        VARCHAR(30)       NOT NULL,
    id_usuario          INT               NOT NULL,
    id_tipo_ocorrencia  INT               NOT NULL,
    id_localizacao      INT               NOT NULL,
    ds_chamado          VARCHAR(MAX)      NOT NULL,
    dt_ocorrencia       DATETIME2         NULL,
    dt_abertura         DATETIME2         NOT NULL CONSTRAINT DF_Ocorrencia_Abertura DEFAULT SYSDATETIME(),
    cd_prioridade       VARCHAR(20)       NOT NULL CONSTRAINT DF_Ocorrencia_Prioridade DEFAULT 'MEDIA',
    cd_status           VARCHAR(30)       NOT NULL CONSTRAINT DF_Ocorrencia_Status DEFAULT 'ABERTO',
    ds_solucao_aplicada VARCHAR(MAX)      NULL,
    dt_conclusao        DATETIME2         NULL,

    CONSTRAINT pk_ocorrencia PRIMARY KEY (id_ocorrencia),
    CONSTRAINT uq_ocorrencia_protocolo UNIQUE (nm_protocolo),
    CONSTRAINT fk_ocorrencia_usuario FOREIGN KEY (id_usuario) REFERENCES Usuario (id_usuario),
    CONSTRAINT fk_ocorrencia_tipo_ocorrencia FOREIGN KEY (id_tipo_ocorrencia) REFERENCES Tipo_ocorrencia (id_tipo_ocorrencia),
    CONSTRAINT fk_ocorrencia_localizacao FOREIGN KEY (id_localizacao) REFERENCES Localizacao (id_localizacao),
    CONSTRAINT CK_Ocorrencia_Prioridade CHECK (cd_prioridade IN ('BAIXA', 'MEDIA', 'ALTA', 'CRITICA')),
    CONSTRAINT CK_Ocorrencia_Status CHECK (cd_status IN ('ABERTO', 'EM_ANDAMENTO', 'CONCLUIDO', 'FECHADO', 'CANCELADO')),
    CONSTRAINT CK_Ocorrencia_Conclusao CHECK (dt_conclusao IS NULL OR dt_conclusao >= dt_abertura)
);
GO

CREATE INDEX IX_Ocorrencia_Usuario ON Ocorrencia (id_usuario);
CREATE INDEX IX_Ocorrencia_Tipo ON Ocorrencia (id_tipo_ocorrencia);
CREATE INDEX IX_Ocorrencia_Localizacao ON Ocorrencia (id_localizacao);
CREATE INDEX IX_Ocorrencia_Status ON Ocorrencia (cd_status, dt_abertura DESC);
GO

CREATE TABLE Anexo (
    id_anexo           INT IDENTITY(1,1) NOT NULL,
    id_ocorrencia      INT               NOT NULL,
    nm_arquivo         VARCHAR(255)      NOT NULL,
    ds_caminho_arquivo VARCHAR(500)      NOT NULL,
    cd_tipo_arquivo    VARCHAR(100)      NOT NULL,
    dt_upload          DATETIME2         NOT NULL CONSTRAINT DF_Anexo_Upload DEFAULT SYSDATETIME(),
    id_usuario         INT               NOT NULL,

    CONSTRAINT pk_anexo PRIMARY KEY (id_anexo),
    CONSTRAINT fk_anexo_ocorrencia FOREIGN KEY (id_ocorrencia) REFERENCES Ocorrencia (id_ocorrencia),
    CONSTRAINT fk_anexo_usuario FOREIGN KEY (id_usuario) REFERENCES Usuario (id_usuario)
);
GO

CREATE INDEX IX_Anexo_Ocorrencia ON Anexo (id_ocorrencia);
CREATE INDEX IX_Anexo_Usuario ON Anexo (id_usuario);
GO

-- Linha do tempo do chamado (gerenciamento pelo administrador).
-- Cada movimentação é um registro novo: nada é apagado ou sobrescrito.
--   ABERTURA    -> chamado aberto pelo cliente
--   STATUS      -> troca de status (guarda o status anterior e o novo)
--   ATUALIZACAO -> atualização sobre o andamento do atendimento
--   DEVOLUTIVA  -> resposta ao cliente (problema identificado, solução aplicada e observações)
CREATE TABLE Movimentacao_ocorrencia (
    id_movimentacao          INT IDENTITY(1,1) NOT NULL,
    id_ocorrencia            INT               NOT NULL,
    id_usuario               INT               NOT NULL,
    cd_tipo_movimentacao     VARCHAR(20)       NOT NULL,
    cd_status_anterior       VARCHAR(30)       NULL,
    cd_status_novo           VARCHAR(30)       NULL,
    ds_movimentacao          VARCHAR(MAX)      NULL,
    ds_problema_identificado VARCHAR(MAX)      NULL,
    ds_solucao_aplicada      VARCHAR(MAX)      NULL,
    ds_observacoes           VARCHAR(MAX)      NULL,
    dt_movimentacao          DATETIME2         NOT NULL CONSTRAINT DF_Movimentacao_Data DEFAULT SYSDATETIME(),

    CONSTRAINT pk_movimentacao_ocorrencia PRIMARY KEY (id_movimentacao),
    CONSTRAINT fk_movimentacao_ocorrencia FOREIGN KEY (id_ocorrencia) REFERENCES Ocorrencia (id_ocorrencia),
    CONSTRAINT fk_movimentacao_usuario FOREIGN KEY (id_usuario) REFERENCES Usuario (id_usuario),
    CONSTRAINT CK_Movimentacao_Tipo CHECK (cd_tipo_movimentacao IN ('ABERTURA', 'STATUS', 'ATUALIZACAO', 'DEVOLUTIVA')),
    CONSTRAINT CK_Movimentacao_StatusAnterior CHECK (cd_status_anterior IS NULL OR cd_status_anterior IN ('ABERTO', 'EM_ANDAMENTO', 'CONCLUIDO', 'FECHADO', 'CANCELADO')),
    CONSTRAINT CK_Movimentacao_StatusNovo CHECK (cd_status_novo IS NULL OR cd_status_novo IN ('ABERTO', 'EM_ANDAMENTO', 'CONCLUIDO', 'FECHADO', 'CANCELADO'))
);
GO

CREATE INDEX IX_Movimentacao_Ocorrencia ON Movimentacao_ocorrencia (id_ocorrencia, dt_movimentacao);
CREATE INDEX IX_Movimentacao_Usuario ON Movimentacao_ocorrencia (id_usuario);
GO

SELECT name FROM sys.tables ORDER BY name;
GO