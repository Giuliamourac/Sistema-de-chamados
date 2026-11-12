(function () {
    'use strict';

    var form = document.getElementById('form-chamado');
    if (!form) return;

    var paineis = Array.prototype.slice.call(form.querySelectorAll('.painel'));
    var passos = Array.prototype.slice.call(form.querySelectorAll('.passo'));
    var botao = document.getElementById('btn-avancar');
    var botaoTexto = document.getElementById('btn-avancar-texto');
    var fotos = document.getElementById('fotos');
    var dica = document.getElementById('upload-dica');
    var dicaOriginal = dica ? dica.textContent : '';

    var TEXTOS = {1: 'Continuar para localização', 2: 'Continuar para contato', 3: 'Finalizar chamado'};
    var MAX_BYTES = 10 * 1024 * 1024;

    var atual = parseInt(form.getAttribute('data-passo-inicial') || '1', 10);
    if (isNaN(atual) || atual < 1 || atual > 3) atual = 1;
    var maior = atual;
    var enviando = false;

    function mostrar(n) {
        atual = n;
        if (n > maior) maior = n;
        paineis.forEach(function (p) {
            p.classList.toggle('painel--ativo', parseInt(p.getAttribute('data-passo'), 10) === n);
        });
        passos.forEach(function (p) {
            p.classList.toggle('passo--ativo', parseInt(p.getAttribute('data-passo'), 10) === n);
        });
        botaoTexto.textContent = TEXTOS[n];
        window.scrollTo({top: 0, behavior: 'smooth'});
    }

    // Valida os campos do painel informado; mostra a mensagem do navegador no primeiro inválido
    function validarPasso(n) {
        var painel = form.querySelector('.painel[data-passo="' + n + '"]');
        var campos = painel.querySelectorAll('input, select, textarea');
        for (var i = 0; i < campos.length; i++) {
            if (campos[i].type === 'file') continue;
            if (campos[i].tagName !== 'SELECT' && campos[i].type !== 'file') {
                campos[i].value = campos[i].value.trim();
            }
            if (!campos[i].checkValidity()) {
                campos[i].reportValidity();
                return false;
            }
        }
        return true;
    }

    function avancar() {
        if (enviando) return;
        if (!validarPasso(atual)) return;
        if (atual < 3) {
            mostrar(atual + 1);
            return;
        }
        // Último passo: confere os anteriores e envia
        for (var n = 1; n < 3; n++) {
            if (!validarPasso(n)) {
                mostrar(n);
                validarPasso(n);
                return;
            }
        }
        enviando = true;
        botao.disabled = true;
        form.submit();
    }

    botao.addEventListener('click', avancar);

    // Enter dentro de um campo também avança, em vez de enviar o formulário incompleto
    form.addEventListener('submit', function (e) {
        e.preventDefault();
        avancar();
    });

    // Permite voltar (ou revisitar) etapas já alcançadas pelo indicador de passos
    passos.forEach(function (p) {
        p.addEventListener('click', function () {
            var n = parseInt(p.getAttribute('data-passo'), 10);
            if (n <= maior) mostrar(n);
        });
    });

    // ---- Máscaras ----
    var cep = document.getElementById('cep');
    if (cep) {
        cep.addEventListener('input', function () {
            var d = cep.value.replace(/\D/g, '').slice(0, 8);
            cep.value = d.length > 5 ? d.slice(0, 5) + '-' + d.slice(5) : d;
        });
    }

    // ---- Busca de endereço pelo CEP (ViaCEP, base dos Correios) ----
    var cepStatus = document.getElementById('cep-status');
    var campoBairro = document.getElementById('bairro');
    var campoLogradouro = document.getElementById('logradouro');
    var ultimoCepBuscado = '';

    function semAcento(t) {
        return (t || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim().toLowerCase();
    }

    function avisoCep(texto, erro) {
        if (!cepStatus) return;
        cepStatus.textContent = texto;
        cepStatus.classList.toggle('campo__dica--erro', !!erro);
    }

    function buscarCep(digitos) {
        if (digitos === ultimoCepBuscado) return;
        ultimoCepBuscado = digitos;
        avisoCep('Buscando endereço...', false);

        var controle = window.AbortController ? new AbortController() : null;
        var limite = setTimeout(function () { if (controle) controle.abort(); }, 8000);

        fetch('https://viacep.com.br/ws/' + digitos + '/json/', controle ? { signal: controle.signal } : undefined)
            .then(function (r) { if (!r.ok) throw new Error('http'); return r.json(); })
            .then(function (d) {
                if (digitos !== cep.value.replace(/\D/g, '')) return; // CEP mudou durante a busca
                if (d.erro) {
                    avisoCep('CEP não encontrado. Preencha o endereço manualmente.', true);
                    return;
                }
                if (campoLogradouro && d.logradouro) {
                    campoLogradouro.value = d.logradouro.slice(0, 150);
                }
                var achou = false;
                if (campoBairro && d.bairro) {
                    var alvo = semAcento(d.bairro);
                    Array.prototype.forEach.call(campoBairro.options, function (o) {
                        if (o.value && semAcento(o.value) === alvo) {
                            campoBairro.value = o.value;
                            achou = true;
                        }
                    });
                }
                var cidade = d.localidade ? ' (' + d.localidade + '/' + d.uf + ')' : '';
                if (d.bairro && !achou) {
                    avisoCep('Endereço preenchido' + cidade + '. O bairro "' + d.bairro + '" não está na lista, selecione o bairro.', true);
                } else {
                    avisoCep('Endereço preenchido' + cidade + '.', false);
                }
            })
            .catch(function () {
                ultimoCepBuscado = '';
                avisoCep('Não foi possível consultar o CEP agora. Preencha o endereço manualmente.', true);
            })
            .then(function () { clearTimeout(limite); });
    }

    if (cep) {
        cep.addEventListener('input', function () {
            var d = cep.value.replace(/\D/g, '');
            if (d.length === 8) {
                buscarCep(d);
            } else {
                ultimoCepBuscado = '';
                avisoCep('', false);
            }
        });
    }

    var telefone = document.getElementById('telefone');
    if (telefone) {
        telefone.addEventListener('input', function () {
            var d = telefone.value.replace(/\D/g, '').slice(0, 11);
            var t = d;
            if (d.length > 6) {
                var corte = d.length > 10 ? 7 : 6;
                t = '(' + d.slice(0, 2) + ') ' + d.slice(2, corte) + '-' + d.slice(corte);
            } else if (d.length > 2) {
                t = '(' + d.slice(0, 2) + ') ' + d.slice(2);
            } else if (d.length > 0) {
                t = '(' + d;
            }
            telefone.value = t;
        });
    }

    // ---- Fotos: PNG/JPG, até 10 MB cada ----
    if (fotos && dica) {
        fotos.addEventListener('change', function () {
            var arquivos = Array.prototype.slice.call(fotos.files);
            var invalido = arquivos.some(function (f) {
                var tipoOk = f.type === 'image/png' || f.type === 'image/jpeg';
                return !tipoOk || f.size > MAX_BYTES;
            });
            if (invalido) {
                fotos.value = '';
                dica.textContent = 'Use fotos PNG ou JPG de até 10 MB cada.';
                return;
            }
            dica.textContent = arquivos.length === 0
                ? dicaOriginal
                : (arquivos.length === 1 ? '1 foto selecionada' : arquivos.length + ' fotos selecionadas');
        });
    }

    mostrar(atual);
})();