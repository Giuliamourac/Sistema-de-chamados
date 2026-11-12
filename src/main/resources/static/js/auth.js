// Tela de cadastro: máscara do telefone e conferência das senhas antes de enviar
(function () {
    var tel = document.getElementById('telefone');
    if (tel) {
        tel.addEventListener('input', function () {
            var d = tel.value.replace(/\D/g, '').slice(0, 11);
            var out = d;
            if (d.length > 10) out = '(' + d.slice(0, 2) + ') ' + d.slice(2, 7) + '-' + d.slice(7);
            else if (d.length > 6) out = '(' + d.slice(0, 2) + ') ' + d.slice(2, 6) + '-' + d.slice(6);
            else if (d.length > 2) out = '(' + d.slice(0, 2) + ') ' + d.slice(2);
            tel.value = out;
        });
    }

    var senha = document.getElementById('senha');
    var confirmar = document.getElementById('confirmarSenha');
    if (senha && confirmar) {
        var checar = function () {
            confirmar.setCustomValidity(confirmar.value && confirmar.value !== senha.value ? 'As senhas não conferem.' : '');
        };
        senha.addEventListener('input', checar);
        confirmar.addEventListener('input', checar);
    }
})();
