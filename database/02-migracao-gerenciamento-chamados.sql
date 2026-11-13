-- =====================================================================
-- Migração: gerenciamento de chamados pelo administrador
-- Rode UMA vez em um banco SABESP que já existe (criado pelo 01-criacao-banco.sql antigo).
-- O script pode ser executado de novo sem erro: cada passo confere se já foi feito.
-- =====================================================================
USE SABESP;
GO

-- 1) Novo status "FECHADO" (atendimento encerrado, sem novas atualizações)
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_Ocorrencia_Status')
    ALTER TABLE Ocorrencia DROP CONSTRAINT CK_Ocorrencia_Status;
GO

ALTER TABLE Ocorrencia ADD CONSTRAINT CK_Ocorrencia_Status
    CHECK (cd_status IN ('ABERTO', 'EM_ANDAMENTO', 'CONCLUIDO', 'FECHADO', 'CANCELADO'));
GO

-- 2) Linha do tempo do chamado
IF OBJECT_ID('Movimentacao_ocorrencia', 'U') IS NULL
BEGIN
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

    CREATE INDEX IX_Movimentacao_Ocorrencia ON Movimentacao_ocorrencia (id_ocorrencia, dt_movimentacao);
    CREATE INDEX IX_Movimentacao_Usuario ON Movimentacao_ocorrencia (id_usuario);
END
GO

-- 3) Chamados que já existiam ganham o registro de abertura na linha do tempo
INSERT INTO Movimentacao_ocorrencia (id_ocorrencia, id_usuario, cd_tipo_movimentacao, cd_status_novo, dt_movimentacao)
SELECT o.id_ocorrencia, o.id_usuario, 'ABERTURA', 'ABERTO', o.dt_abertura
FROM Ocorrencia o
WHERE NOT EXISTS (
    SELECT 1 FROM Movimentacao_ocorrencia m
    WHERE m.id_ocorrencia = o.id_ocorrencia AND m.cd_tipo_movimentacao = 'ABERTURA'
);
GO

-- 4) Tornar um usuário administrador
-- Cadastre a conta normalmente pela tela /cadastro (a senha é gravada com hash pela aplicação)
-- e depois troque o perfil dela, trocando o e-mail abaixo:
-- UPDATE Usuario SET cd_tipo_usuario = 'ADMIN' WHERE nm_email = 'admin@exemplo.com';
-- GO

SELECT name FROM sys.tables ORDER BY name;
GO
