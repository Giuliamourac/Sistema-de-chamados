package com.sabesp.chamados.controller;

import com.sabesp.chamados.dto.ChamadoForm;
import com.sabesp.chamados.model.Chamado;
import com.sabesp.chamados.service.ChamadoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/chamados")
public class ChamadoController {

    private static final List<String> BAIRROS = List.of(
            "Aparecida", "Boqueirão", "Campo Grande", "Centro", "Chico de Paula", "Embaré",
            "Encruzilhada", "Estuário", "Gonzaga", "Jabaquara", "José Menino", "Macuco",
            "Marapé", "Paquetá", "Pompeia", "Ponta da Praia", "Saboó", "Valongo",
            "Vila Belmiro", "Vila Mathias", "Vila Nova"
    );

    private static final Pattern CEP = Pattern.compile("\\d{5}-?\\d{3}");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private record Erro(int passo, String mensagem) {
    }

    private final ChamadoService chamadoService;

    public ChamadoController(ChamadoService chamadoService) {
        this.chamadoService = chamadoService;
    }

    // Tela "Acompanhe suas solicitações" (somente as em andamento)
    @GetMapping
    public String acompanhar(Model model) {
        model.addAttribute("chamados", chamadoService.listarEmAndamento());
        return "chamados/acompanhar";
    }

    // Tela "Histórico de solicitações" (todas, com busca opcional)
    @GetMapping("/historico")
    public String historico(@RequestParam(required = false) String q, Model model) {
        boolean temBusca = q != null && !q.isBlank();
        model.addAttribute("chamados", temBusca ? chamadoService.buscar(q.trim()) : chamadoService.listarTodos());
        model.addAttribute("q", q);
        return "chamados/historico";
    }

    // Formulário de abertura (3 passos na mesma página)
    @GetMapping("/novo")
    public String novo(Model model) {
        return telaFormulario(model, new ChamadoForm(), null, 1);
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("form") ChamadoForm form,
                         @RequestParam(value = "fotos", required = false) List<MultipartFile> fotos,
                         Model model) {

        Erro erro = validar(form, chamadoService.listarTiposAtivos());
        if (erro == null) {
            String erroFotos = chamadoService.validarFotos(fotos);
            if (erroFotos != null) {
                erro = new Erro(1, erroFotos);
            }
        }
        if (erro != null) {
            return telaFormulario(model, form, erro.mensagem(), erro.passo());
        }

        // Normaliza o CEP para 00000-000 (coluna CHAR(9) com CHECK no banco)
        String cep = form.getCep().trim().replace("-", "");
        form.setCep(cep.substring(0, 5) + "-" + cep.substring(5));

        Chamado salvo = chamadoService.abrirChamado(form, fotos);
        return "redirect:/chamados/sucesso/" + salvo.getProtocolo();
    }

    // Foto maior que o limite configurado em spring.servlet.multipart.max-file-size
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String arquivoGrande(Model model) {
        return telaFormulario(model, new ChamadoForm(), "Cada foto deve ter no máximo 10 MB.", 1);
    }

    // Tela depois de registrar o chamado
    @GetMapping("/sucesso/{protocolo}")
    public String sucesso(@PathVariable String protocolo, Model model) {
        return chamadoService.buscarPorProtocolo(protocolo)
                .map(chamado -> {
                    model.addAttribute("chamado", chamado);
                    model.addAttribute("sucesso", true);
                    return "chamados/detalhe";
                })
                .orElse("redirect:/chamados/novo");
    }

    // Botão "Conferir andamento"
    @GetMapping("/acompanhar/{protocolo}")
    public String detalhe(@PathVariable String protocolo, Model model) {
        return chamadoService.buscarPorProtocolo(protocolo)
                .map(chamado -> {
                    model.addAttribute("chamado", chamado);
                    model.addAttribute("sucesso", false);
                    return "chamados/detalhe";
                })
                .orElse("redirect:/chamados");
    }

    private String telaFormulario(Model model, ChamadoForm form, String erro, int passo) {
        model.addAttribute("form", form);
        model.addAttribute("tipos", chamadoService.listarTiposAtivos());
        model.addAttribute("bairros", BAIRROS);
        model.addAttribute("erro", erro);
        model.addAttribute("passoInicial", passo);
        return "chamados/form";
    }

    // Validação no servidor (limites iguais aos das colunas do banco)
    private Erro validar(ChamadoForm f, List<String> tiposAtivos) {
        if (vazio(f.getTipoOcorrencia()) || vazio(f.getDescricao())) {
            return new Erro(1, "Preencha o tipo de problema e a descrição.");
        }
        if (tiposAtivos.stream().noneMatch(t -> t.equalsIgnoreCase(f.getTipoOcorrencia().trim()))) {
            return new Erro(1, "Selecione um tipo de problema válido.");
        }

        if (vazio(f.getCep()) || !CEP.matcher(f.getCep().trim()).matches()) {
            return new Erro(2, "Informe um CEP válido (00000-000).");
        }
        if (vazio(f.getBairro()) || !BAIRROS.contains(f.getBairro().trim())) {
            return new Erro(2, "Selecione o bairro.");
        }
        if (vazio(f.getLogradouro()) || vazio(f.getPontoReferencia())) {
            return new Erro(2, "Preencha o logradouro e o ponto de referência.");
        }
        if (f.getLogradouro().trim().length() > 150) {
            return new Erro(2, "O logradouro aceita no máximo 150 caracteres.");
        }
        if (f.getPontoReferencia().trim().length() > 255) {
            return new Erro(2, "O ponto de referência aceita no máximo 255 caracteres.");
        }

        if (vazio(f.getNomeSolicitante()) || f.getNomeSolicitante().trim().length() > 120) {
            return new Erro(3, "Informe seu nome completo (até 120 caracteres).");
        }
        if (vazio(f.getEmail()) || f.getEmail().trim().length() > 150
                || !EMAIL.matcher(f.getEmail().trim()).matches()) {
            return new Erro(3, "Informe um e-mail válido.");
        }
        String digitosTelefone = f.getTelefone() == null ? "" : f.getTelefone().replaceAll("\\D", "");
        if (digitosTelefone.length() < 10 || digitosTelefone.length() > 11) {
            return new Erro(3, "Informe um telefone válido com DDD.");
        }
        return null;
    }

    private boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }
}