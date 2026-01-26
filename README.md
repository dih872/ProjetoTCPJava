# Ludo TCP - Jogo em Rede

## Integrantes da Equipe

- Ana Vitória Araújo Albuquerque
- Diego Santana Estevão
- Layane de Santana Soares
- Raíssa Marta de Santana
- Vivian Ferreira de Melo

## Descrição do Jogo

Ludo TCP é uma implementação do jogo clássico Ludo desenvolvida em Java com sockets TCP. O jogo permite que três jogadores compitam entre si por meio de uma conexão de rede.

**Objetivos do Jogo:**
- Cada jogador controla quatro peças.
- O objetivo é mover todas as quatro peças até o centro do tabuleiro.
- O primeiro jogador a levar todas as peças ao centro vence.
- As peças começam na base e só saem com uma jogada de valor 5.
- As peças podem capturar peças adversárias, enviando-as de volta à base.

## Como Executar o Projeto

### Passos para Compilar

1. Abra o projeto no VS Code.
2. Você pode compilar automaticamente indo na parte de executar.
3. Sempre inicie primeiro o arquivo LudoTCPServer.java.

### Passos para Executar

1. **Inicie o servidor:**
   - Execute o arquivo LudoTCPServer.java.
   - O servidor aguardará três clientes se conectarem.

2. **Inicie os clientes (em  terminais diferentes):**
   - Abra o arquivo LudoTCPClient.java e execute-o.
   - Após cada cliente se conectar, digite seu nome.
   - Repita o processo três vezes para os três jogadores.

## Como Jogar

### Fluxo do Jogo

1. **Conexão:** Cada cliente se conecta ao servidor e registra seu nome.
   - Depois disso, o servidor irá perguntar se poderá iniciar a partida.
2. **Turno do jogador:** Na sua vez:
   - Digite `girar` para rolar o dado (resultado: 1 a 6).
   - Escolha uma peça para mover (0-3) ou digite `skip` se não puder mover.
   
3. **Movimentação de peças:**
   - Peças na base só saem com resultado 5.
   - Peças no tabuleiro se movem conforme a quantidade rolada.
   - Se passar do centro, a peça permanece na mesma posição.
   - É necessário chegar ao centro com o valor exato.

4. **Captura:**
   - Se sua peça cair na mesma posição de uma peça adversária, ela é capturada.
   - A peça capturada volta à base.

5. **Dados:**
   - Se rolar 6, o jogador tem direito a outro turno.
   - Caso contrário, a vez passa para o próximo jogador.

6. **Vitória:**
   - O primeiro a levar todas as quatro peças ao centro vence.
   - Ao final, os jogadores são questionados se desejam jogar novamente.

### Controles
- `girar` - Rolar o dado
- `0`, `1`, `2`, `3` - Selecionar qual peça mover
- `skip` - Pular o turno (quando não consegue mover)
- `sim` ou `s` - Jogar novamente
- `não` - Sair do jogo
