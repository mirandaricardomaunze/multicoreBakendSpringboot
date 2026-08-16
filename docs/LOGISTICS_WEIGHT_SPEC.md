# Peso Logistico em Pedidos e Guias

## Cadastro

- `netUnitWeightKg`: peso liquido de uma unidade, em kg.
- `grossUnitWeightKg`: peso da unidade com embalagem, em kg; deve ser maior ou igual ao liquido.
- Peso bruto por caixa = `grossUnitWeightKg * unitsPerBox`.

## Documentos

- Peso da linha = quantidade total * peso bruto unitario.
- Percentagem de quantidade = quantidade da linha / soma das quantidades * 100.
- Percentagem de peso = peso da linha / peso bruto total * 100.
- Pedidos e guias mostram quantidade, composicao em caixas, peso da linha e ambas percentagens.
- O peso total da guia e a referencia usada pelo responsavel para seleccionar a viatura.

Pesos ausentes valem zero e aparecem como "nao configurado"; nunca sao inferidos do nome do produto.
