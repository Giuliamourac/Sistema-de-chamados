# Sistema-Chamados
Sistema de chamados de saneamento desenvolvido em Java com Spring Boot e SQL Server, voltado ao registro e gerenciamento de ocorrências relacionadas aos serviços de água e esgoto. O projeto permite organizar chamados, informações de localização, usuários e anexos.


## Login e cadastro
- Telas: `/login` e `/cadastro`; sair: botão "Sair" do cabeçalho (`POST /logout`).
- Senhas gravadas com hash PBKDF2-SHA256 em `Usuario.cd_senha` (nunca em texto puro); sessão via `HttpSession`.
- Perfil `dev`: usuário de teste `maria.teste@exemplo.com` / `senha1234`.
