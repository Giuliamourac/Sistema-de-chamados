// Painel do administrador: aplica o filtro assim que uma opção é marcada/desmarcada.
// A filtragem continua sendo feita no servidor (GET /admin/chamados?status=...&tempo=...).
(function () {
    var form = document.getElementById('form-filtro');
    if (!form) return;

    form.querySelectorAll('input[type="checkbox"]').forEach(function (caixa) {
        caixa.addEventListener('change', function () {
            form.submit();
        });
    });

    // Busca: aplica ao parar de digitar (Enter também funciona)
    var busca = form.querySelector('input[name="q"]');
    var espera;
    if (busca) {
        busca.addEventListener('input', function () {
            clearTimeout(espera);
            espera = setTimeout(function () { form.submit(); }, 600);
        });
        // Mantém o cursor no fim do texto depois que a página recarrega
        if (busca.value) {
            busca.focus();
            busca.setSelectionRange(busca.value.length, busca.value.length);
        }
    }
})();
