(() => {
    const ENDERECO_DA_API = '/api/painel';
    const INTERVALO_DE_ATUALIZACAO = 60000;
    const MILISSEGUNDOS_POR_DIA = 86400000;

    const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
    const inteiro = new Intl.NumberFormat('pt-BR');
    const umaCasa = new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 });
    const horario = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit', second: '2-digit' });

    const NOMES_DOS_STATUS = {
        RECEBIDO: 'Recebido',
        EM_PREPARACAO: 'Em preparação',
        PRONTO: 'Pronto',
        SAIU_PARA_ENTREGA: 'Saiu para entrega',
        RETIRADO: 'Retirado',
        FINALIZADO: 'Finalizado',
        CANCELADO: 'Cancelado'
    };

    const CORES_DOS_STATUS = {
        RECEBIDO: '#6c757d',
        EM_PREPARACAO: '#fd7e14',
        PRONTO: '#ffc107',
        SAIU_PARA_ENTREGA: '#0dcaf0',
        RETIRADO: '#6f42c1',
        FINALIZADO: '#198754',
        CANCELADO: '#dc3545'
    };

    const CORES = {
        principal: '#0d6efd',
        principalSuave: 'rgba(13, 110, 253, 0.12)',
        destaque: 'rgba(253, 126, 20, 0.45)',
        barra: 'rgba(13, 110, 253, 0.55)',
        barraDeDestaque: '#0d6efd',
        cancelamento: 'rgba(220, 53, 69, 0.7)'
    };

    const campo = (id) => document.getElementById(id);
    const filtros = {
        formulario: campo('filtrosDoPainel'),
        atalho: campo('atalhoDePeriodo'),
        de: campo('filtroDe'),
        ate: campo('filtroAte'),
        tipoSaida: campo('filtroTipoSaida'),
        aviso: campo('avisoDosFiltros'),
        limpar: campo('botaoLimparFiltros')
    };
    const controles = {
        granularidade: campo('granularidadeDaSerie'),
        dimensao: campo('dimensaoDaDistribuicao'),
        criterio: campo('criterioDoRanking'),
        categoria: campo('categoriaDoRanking'),
        limite: campo('limiteDoRanking'),
        atualizar: campo('botaoAtualizar'),
        automatica: campo('atualizacaoAutomatica'),
        ultimaAtualizacao: campo('ultimaAtualizacao')
    };

    const graficos = {};
    let diasDoPeriodoMaximo = 366;
    let filtrosAplicados = null;
    let atualizacaoEmCurso = false;
    let temporizador = null;

    function dataIso(data) {
        const mes = String(data.getMonth() + 1).padStart(2, '0');
        const dia = String(data.getDate()).padStart(2, '0');
        return `${data.getFullYear()}-${mes}-${dia}`;
    }

    function dataBrasileira(iso) {
        const [ano, mes, dia] = iso.split('-');
        return `${dia}/${mes}/${ano}`;
    }

    function aplicarAtalho(atalho) {
        if (atalho === 'personalizado') {
            return;
        }
        const hoje = new Date();
        let inicio = new Date(hoje);
        if (atalho === 'mes') {
            inicio = new Date(hoje.getFullYear(), hoje.getMonth(), 1);
        } else if (atalho !== 'hoje') {
            inicio.setDate(hoje.getDate() - (Number(atalho) - 1));
        }
        filtros.de.value = dataIso(inicio);
        filtros.ate.value = dataIso(hoje);
    }

    function lerFiltros() {
        const de = filtros.de.value;
        const ate = filtros.ate.value;
        if (!de || !ate) {
            throw new Error('Informe as duas datas do período.');
        }
        if (de > ate) {
            throw new Error('A data inicial não pode ser posterior à data final.');
        }
        const dias = (Date.parse(ate) - Date.parse(de)) / MILISSEGUNDOS_POR_DIA + 1;
        if (dias > diasDoPeriodoMaximo) {
            throw new Error(`O período pode ter no máximo ${diasDoPeriodoMaximo} dias.`);
        }
        return { de, ate, tipoSaida: filtros.tipoSaida.value };
    }

    function mostrarAviso(mensagem) {
        filtros.aviso.textContent = mensagem;
        filtros.aviso.classList.toggle('d-none', !mensagem);
    }

    function consulta(extras = {}) {
        const parametros = new URLSearchParams({ de: filtrosAplicados.de, ate: filtrosAplicados.ate });
        if (filtrosAplicados.tipoSaida) {
            parametros.set('tipoSaida', filtrosAplicados.tipoSaida);
        }
        Object.entries(extras)
            .filter(([, valor]) => valor !== '' && valor !== null && valor !== undefined)
            .forEach(([nome, valor]) => parametros.set(nome, valor));
        return parametros.toString();
    }

    async function buscar(caminho) {
        let resposta;
        try {
            resposta = await fetch(`${ENDERECO_DA_API}${caminho}`, { headers: { Accept: 'application/json' } });
        } catch (falhaDeConexao) {
            throw new Error('Não foi possível conectar ao servidor.');
        }
        if (resposta.status === 401) {
            window.location.assign('/login');
            throw new Error('Sua sessão expirou. Faça login novamente.');
        }
        if (!resposta.ok) {
            const mensagem = resposta.status === 403
                ? 'Você não tem permissão para consultar estes dados.'
                : `Não foi possível carregar os dados (erro ${resposta.status}).`;
            const corpo = await resposta.json().catch(() => ({}));
            throw new Error(corpo.mensagem || mensagem);
        }
        return resposta.json();
    }

    async function carregarBlocos(idsDosBlocos, caminho, desenhar) {
        const blocos = idsDosBlocos.map(campo);
        blocos.forEach((bloco) => { bloco.dataset.estado = 'carregando'; });
        try {
            const dados = await buscar(caminho);
            const conteudo = desenhar(dados);
            blocos.forEach((bloco) => {
                const possuiConteudo = typeof conteudo === 'object' ? conteudo[bloco.id] : conteudo;
                bloco.dataset.estado = possuiConteudo ? 'pronto' : 'vazio';
            });
            return true;
        } catch (erro) {
            blocos.forEach((bloco) => {
                bloco.dataset.estado = 'erro';
                bloco.querySelector('.mensagem-do-erro').textContent = erro.message;
            });
            return false;
        }
    }

    function desenharGrafico(nome, idDoCanvas, configuracao) {
        if (graficos[nome]) {
            graficos[nome].destroy();
        }
        graficos[nome] = new Chart(campo(idDoCanvas), configuracao);
    }

    function telaEstreita() {
        return window.matchMedia('(max-width: 576px)').matches;
    }

    function horasComMovimento(fatias) {
        const comPedidos = fatias.map((fatia, indice) => (fatia.pedidos > 0 ? indice : -1)).filter((indice) => indice >= 0);
        if (comPedidos.length === 0) {
            return fatias;
        }
        const inicio = Math.max(0, comPedidos[0] - 1);
        const fim = Math.min(fatias.length - 1, comPedidos[comPedidos.length - 1] + 1);
        return fatias.slice(inicio, fim + 1);
    }

    function opcoesBasicas(extras) {
        return Object.assign({
            responsive: true,
            maintainAspectRatio: false,
            animation: { duration: 400 },
            legend: { display: false }
        }, extras);
    }

    function preencherIndicador(nome, texto, dica) {
        document.querySelectorAll(`[data-indicador="${nome}"]`).forEach((elemento) => {
            elemento.textContent = texto;
            elemento.title = dica || '';
        });
    }

    function preencherVariacao(nome, variacao) {
        const elemento = document.querySelector(`[data-variacao="${nome}"]`);
        elemento.classList.remove('variacao-alta', 'variacao-baixa');
        if (variacao === null || variacao === undefined) {
            elemento.textContent = 'Sem movimento no período anterior';
            return;
        }
        const seta = variacao > 0 ? '▲' : variacao < 0 ? '▼' : '■';
        elemento.textContent = `${seta} ${umaCasa.format(Math.abs(variacao))}% vs. período anterior`;
        if (variacao !== 0) {
            elemento.classList.add(variacao > 0 ? 'variacao-alta' : 'variacao-baixa');
        }
    }

    function minutos(valor) {
        return valor === null || valor === undefined ? '—' : `${inteiro.format(valor)} min`;
    }

    function desenharIndicadores(dados) {
        const comparativo = dados.comparativo;
        preencherIndicador('faturamento', moeda.format(dados.faturamento));
        preencherIndicador('pedidosFinalizados', inteiro.format(dados.pedidosFinalizados));
        preencherIndicador('ticketMedio', moeda.format(dados.ticketMedio));
        preencherIndicador('descontosConcedidos', moeda.format(dados.descontosConcedidos));
        preencherIndicador('pedidosCancelados', inteiro.format(dados.pedidosCancelados));
        preencherIndicador('taxaDeCancelamento', `${umaCasa.format(dados.taxaDeCancelamento)}% dos pedidos encerrados`);
        const semHistorico = 'Sem histórico de status para calcular neste período';
        preencherIndicador('tempoMedioDePreparoEmMinutos', minutos(dados.tempoMedioDePreparoEmMinutos),
            dados.tempoMedioDePreparoEmMinutos === null ? semHistorico : '');
        preencherIndicador('tempoMedioDeAtendimentoEmMinutos', minutos(dados.tempoMedioDeAtendimentoEmMinutos),
            dados.tempoMedioDeAtendimentoEmMinutos === null ? semHistorico : '');
        preencherIndicador('periodoAnterior',
            `${dataBrasileira(comparativo.periodoAnterior.de)} a ${dataBrasileira(comparativo.periodoAnterior.ate)}`);
        preencherIndicador('faturamentoAnterior', `Faturamento anterior: ${moeda.format(comparativo.faturamentoAnterior)}`);
        preencherVariacao('variacaoDoFaturamento', comparativo.variacaoDoFaturamento);
        preencherVariacao('variacaoDosPedidos', comparativo.variacaoDosPedidos);
        preencherVariacao('variacaoDoTicketMedio', comparativo.variacaoDoTicketMedio);
        return true;
    }

    function desenharSerie(dados) {
        const pontos = dados.pontos;
        desenharGrafico('serie', 'graficoSerie', {
            type: 'bar',
            data: {
                labels: pontos.map((ponto) => ponto.rotulo),
                datasets: [
                    {
                        type: 'line',
                        label: 'Faturamento',
                        data: pontos.map((ponto) => ponto.faturamento),
                        yAxisID: 'valores',
                        borderColor: CORES.principal,
                        backgroundColor: CORES.principalSuave,
                        pointRadius: pontos.length > 45 ? 0 : 3,
                        lineTension: 0.25,
                        fill: true,
                        order: 1
                    },
                    {
                        type: 'bar',
                        label: 'Pedidos',
                        data: pontos.map((ponto) => ponto.pedidos),
                        yAxisID: 'quantidades',
                        backgroundColor: CORES.destaque,
                        order: 2
                    }
                ]
            },
            options: opcoesBasicas({
                legend: { display: true, position: 'bottom' },
                tooltips: {
                    mode: 'index',
                    intersect: false,
                    callbacks: {
                        label: (item, grafico) => {
                            const serie = grafico.datasets[item.datasetIndex].label;
                            const valor = item.datasetIndex === 0 ? moeda.format(item.yLabel) : inteiro.format(item.yLabel);
                            return `${serie}: ${valor}`;
                        }
                    }
                },
                scales: {
                    xAxes: [{ gridLines: { display: false }, ticks: { maxRotation: 0, autoSkip: true, maxTicksLimit: telaEstreita() ? 5 : 12 } }],
                    yAxes: [
                        { id: 'valores', position: 'left', ticks: { beginAtZero: true, callback: (valor) => moeda.format(valor) } },
                        { id: 'quantidades', position: 'right', gridLines: { drawOnChartArea: false }, ticks: { beginAtZero: true, precision: 0 } }
                    ]
                }
            })
        });
        return pontos.some((ponto) => ponto.pedidos > 0);
    }

    function desenharSituacao(dados) {
        const comPedidos = dados.noPeriodo.filter((item) => item.quantidade > 0);
        desenharGrafico('status', 'graficoStatus', {
            type: 'doughnut',
            data: {
                labels: comPedidos.map((item) => NOMES_DOS_STATUS[item.status]),
                datasets: [{
                    data: comPedidos.map((item) => item.quantidade),
                    backgroundColor: comPedidos.map((item) => CORES_DOS_STATUS[item.status])
                }]
            },
            options: opcoesBasicas({
                cutoutPercentage: 60,
                legend: { display: true, position: 'bottom', labels: { boxWidth: 12 } },
                tooltips: {
                    callbacks: {
                        label: (item, grafico) => {
                            const quantidade = grafico.datasets[0].data[item.index];
                            const percentual = umaCasa.format(quantidade * 100 / dados.totalNoPeriodo);
                            return `${grafico.labels[item.index]}: ${inteiro.format(quantidade)} (${percentual}%)`;
                        }
                    }
                }
            })
        });

        const lista = campo('listaEmAndamento');
        lista.replaceChildren(...dados.emAndamentoAgora.map((item) => {
            const linha = document.createElement('li');
            linha.className = 'list-group-item d-flex justify-content-between align-items-center px-0';
            const nome = document.createElement('span');
            const marcador = document.createElement('i');
            marcador.className = 'bi bi-circle-fill me-2 small';
            marcador.style.color = CORES_DOS_STATUS[item.status];
            nome.append(marcador, NOMES_DOS_STATUS[item.status]);
            const quantidade = document.createElement('span');
            quantidade.className = item.quantidade > 0 ? 'fw-semibold' : 'text-muted';
            quantidade.textContent = inteiro.format(item.quantidade);
            linha.append(nome, quantidade);
            return linha;
        }));
        campo('totalEmAndamento').textContent = inteiro.format(dados.totalEmAndamentoAgora);

        return {
            blocoStatus: dados.totalNoPeriodo > 0,
            blocoEmAndamento: dados.totalEmAndamentoAgora > 0
        };
    }

    function celula(texto, classe) {
        const elemento = document.createElement('td');
        elemento.textContent = texto;
        if (classe) {
            elemento.className = classe;
        }
        return elemento;
    }

    function desenharRanking(dados) {
        const porFaturamento = dados.criterio === 'FATURAMENTO';
        const medida = (produto) => (porFaturamento ? produto.faturamento : produto.quantidade);
        const lider = dados.produtos.length > 0 ? medida(dados.produtos[0]) : 0;

        campo('tabelaRanking').replaceChildren(...dados.produtos.map((produto) => {
            const linha = document.createElement('tr');
            const proporcao = lider > 0 ? Math.round(medida(produto) * 100 / lider) : 0;
            const barra = document.createElement('div');
            barra.className = 'progress';
            barra.style.height = '8px';
            barra.title = `${proporcao}% do 1º colocado`;
            const preenchimento = document.createElement('div');
            preenchimento.className = 'progress-bar';
            preenchimento.style.width = `${proporcao}%`;
            barra.append(preenchimento);
            const colunaDaBarra = document.createElement('td');
            colunaDaBarra.className = 'd-none d-md-table-cell';
            colunaDaBarra.append(barra);

            linha.append(
                celula(`${produto.posicao}º`, 'text-center text-muted'),
                celula(produto.nome, 'fw-medium'),
                celula(produto.categoria, 'text-muted d-none d-md-table-cell'),
                celula(inteiro.format(produto.quantidade), 'text-end'),
                celula(moeda.format(produto.faturamento), 'text-end'),
                colunaDaBarra);
            return linha;
        }));
        return dados.produtos.length > 0;
    }

    function desenharDistribuicao(dados) {
        const fatias = dados.dimensao === 'HORA' ? horasComMovimento(dados.fatias) : dados.fatias;
        const maior = Math.max(...fatias.map((fatia) => fatia.pedidos));
        desenharGrafico('distribuicao', 'graficoDistribuicao', {
            type: 'bar',
            data: {
                labels: fatias.map((fatia) => fatia.rotulo),
                datasets: [{
                    label: 'Pedidos finalizados',
                    data: fatias.map((fatia) => fatia.pedidos),
                    backgroundColor: fatias.map((fatia) => (fatia.pedidos === maior && maior > 0 ? CORES.barraDeDestaque : CORES.barra))
                }]
            },
            options: opcoesBasicas({
                tooltips: {
                    callbacks: {
                        label: (item) => {
                            const fatia = fatias[item.index];
                            return `${inteiro.format(fatia.pedidos)} pedidos · ${moeda.format(fatia.faturamento)}`;
                        }
                    }
                },
                scales: {
                    xAxes: [{ gridLines: { display: false } }],
                    yAxes: [{ ticks: { beginAtZero: true, precision: 0 } }]
                }
            })
        });
        return maior > 0;
    }

    function desenharCancelamentos(dados) {
        campo('totalCancelamentos').textContent = inteiro.format(dados.total);
        desenharGrafico('cancelamentos', 'graficoCancelamentos', {
            type: 'horizontalBar',
            data: {
                labels: dados.motivos.map((motivo) => motivo.descricao),
                datasets: [{
                    label: 'Cancelamentos',
                    data: dados.motivos.map((motivo) => motivo.quantidade),
                    backgroundColor: CORES.cancelamento
                }]
            },
            options: opcoesBasicas({
                tooltips: { callbacks: { label: (item) => `${inteiro.format(item.xLabel)} cancelamento(s)` } },
                scales: {
                    xAxes: [{ ticks: { beginAtZero: true, precision: 0 } }],
                    yAxes: [{ gridLines: { display: false } }]
                }
            })
        });
        return dados.total > 0;
    }

    const carregadores = {
        blocoIndicadores: () => carregarBlocos(['blocoIndicadores'], `/indicadores?${consulta()}`, desenharIndicadores),
        blocoSerie: () => carregarBlocos(['blocoSerie'],
            `/vendas/serie?${consulta({ granularidade: controles.granularidade.value })}`, desenharSerie),
        blocoStatus: () => carregarBlocos(['blocoStatus', 'blocoEmAndamento'], `/pedidos/situacao?${consulta()}`, desenharSituacao),
        blocoRanking: () => carregarBlocos(['blocoRanking'], `/produtos/mais-vendidos?${consulta({
            ordenarPor: controles.criterio.value,
            categoria: controles.categoria.value,
            limite: controles.limite.value
        })}`, desenharRanking),
        blocoDistribuicao: () => carregarBlocos(['blocoDistribuicao'],
            `/vendas/distribuicao?${consulta({ dimensao: controles.dimensao.value })}`, desenharDistribuicao),
        blocoCancelamentos: () => carregarBlocos(['blocoCancelamentos'], `/pedidos/cancelamentos?${consulta()}`, desenharCancelamentos)
    };
    carregadores.blocoEmAndamento = carregadores.blocoStatus;

    async function atualizarTudo() {
        if (atualizacaoEmCurso || !filtrosAplicados) {
            return;
        }
        atualizacaoEmCurso = true;
        controles.atualizar.disabled = true;
        const unicos = [...new Set(Object.values(carregadores))];
        const resultados = await Promise.all(unicos.map((carregar) => carregar()));
        const falhas = resultados.filter((sucesso) => !sucesso).length;
        const momento = horario.format(new Date());
        controles.ultimaAtualizacao.textContent = falhas === 0
            ? `Atualizado às ${momento}`
            : `Atualizado às ${momento} · ${falhas} bloco(s) com falha`;
        controles.atualizar.disabled = false;
        atualizacaoEmCurso = false;
    }

    function aplicarFiltros() {
        try {
            filtrosAplicados = lerFiltros();
            mostrarAviso('');
            atualizarTudo();
        } catch (filtroInvalido) {
            mostrarAviso(filtroInvalido.message);
        }
    }

    function programarAtualizacao() {
        clearInterval(temporizador);
        if (controles.automatica.checked) {
            temporizador = setInterval(() => {
                if (!document.hidden) {
                    atualizarTudo();
                }
            }, INTERVALO_DE_ATUALIZACAO);
        }
    }

    async function carregarOpcoesDeFiltro() {
        try {
            const opcoes = await buscar('/filtros');
            diasDoPeriodoMaximo = opcoes.diasDoPeriodoMaximo;
            filtros.de.value = opcoes.periodoPadrao.de;
            filtros.ate.value = opcoes.periodoPadrao.ate;
            opcoes.categorias.forEach((categoria) => controles.categoria.add(new Option(categoria, categoria)));
        } catch (erro) {
            mostrarAviso(`Não foi possível carregar as opções de filtro (${erro.message}). Usando os últimos 30 dias.`);
        }
    }

    function ligarEventos() {
        filtros.formulario.addEventListener('submit', (evento) => {
            evento.preventDefault();
            aplicarFiltros();
        });
        filtros.atalho.addEventListener('change', () => {
            aplicarAtalho(filtros.atalho.value);
            if (filtros.atalho.value !== 'personalizado') {
                aplicarFiltros();
            }
        });
        [filtros.de, filtros.ate].forEach((data) => data.addEventListener('input', () => {
            filtros.atalho.value = 'personalizado';
        }));
        filtros.tipoSaida.addEventListener('change', aplicarFiltros);
        filtros.limpar.addEventListener('click', () => {
            filtros.atalho.value = '30';
            filtros.tipoSaida.value = '';
            aplicarAtalho('30');
            aplicarFiltros();
        });
        controles.granularidade.addEventListener('change', carregadores.blocoSerie);
        controles.dimensao.addEventListener('change', carregadores.blocoDistribuicao);
        [controles.criterio, controles.categoria, controles.limite]
            .forEach((controle) => controle.addEventListener('change', carregadores.blocoRanking));
        controles.atualizar.addEventListener('click', atualizarTudo);
        controles.automatica.addEventListener('change', programarAtualizacao);
        document.querySelectorAll('#painelDeVendas .botao-repetir').forEach((botao) => {
            botao.addEventListener('click', () => {
                const bloco = botao.closest('.bloco-do-painel');
                carregadores[bloco.id]();
            });
        });
    }

    async function iniciar() {
        aplicarAtalho('30');
        ligarEventos();
        await carregarOpcoesDeFiltro();
        aplicarFiltros();
        programarAtualizacao();
    }

    iniciar();
})();
