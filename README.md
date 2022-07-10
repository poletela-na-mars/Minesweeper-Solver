# Minesweeper-Solver
Minesweeper. With the possibility of solving fields of specified sizes

**Правила игры:**
плоское игровое поле разделено на смежные ячейки некоторые из которых «заминированы»; количество «заминированных» ячеек известно. Целью игры является открытие всех ячеек, не содержащих мины.
Игрок открывает ячейки, стараясь не открыть ячейку с миной. Открыв ячейку с миной, он проигрывает. Мины расставляются после первого хода, поэтому проиграть на первом же ходу невозможно. Если под открытой ячейкой мины нет, то в ней появляется число, показывающее, сколько ячеек, соседствующих с только что открытой, «заминировано»; используя эти числа, игрок пытается рассчитать расположение мин, однако иногда даже в середине и в конце игры некоторые ячейки всё же приходится открывать наугад. Если под соседними ячейками тоже нет мин, то открывается некоторая «не заминированная» область до ячеек, в которых есть цифры. «Заминированные» ячейки игрок может пометить, чтобы случайно не открыть их. Открыв все «не заминированные» ячейки, игрок выигрывает.

**Игра:** при запуске игры игроку будет предложено задать размеры поля и число мин в начальном окне.

<p align="center">
<img src="https://user-images.githubusercontent.com/70761083/178152642-2962077e-9028-4c45-b620-de6e31eb2c52.png" width="300" />
</p>

После инициализации поля игра начинается. Справа на панели можно видеть флаг и две кнопки, «Hint» и «Solver». 
С помощью флага можно отметить ячейки, которые кажутся «заминированными». Кнопка «Hint» становится активной после первого клика. По ее нажатии можно получить подсказку, а именно выставленный флаг на ячейке, где точно есть мина. 
Кнопка «Solver» инициирует автоматическое решение поля до победы или проигрыша. Автоматическое решение может быть вызвано как сразу, так и после ходов игрока. По ходу решения солвер будет сам выставлять флаги, где вероятнее всего нет мин. 

<p align="center">
<img src="https://user-images.githubusercontent.com/70761083/178152781-216d6085-3c92-456e-bbf1-4212dda38222.png" width="300" />
</p>

Также поле может быть весьма большим, например, 100 на 100 (на рисунке ниже). В игре есть ограничения на поле, например, минимальное поле – не меньше 2 на 2, а также не может быть меньше одной мины. Число мин не может быть больше чем число ячеек или быть равным ему.

<p align="center">
<img src="https://user-images.githubusercontent.com/70761083/178152891-8af72572-c8d1-487e-ae1f-28a570f7fa67.png" width="300" />
</p>

После выигрыша или проигрыша мы получим уведомление об этом, а также число совершенных нами ходов за игру.


<p align="center">
<img src="https://user-images.githubusercontent.com/70761083/178152980-9fb55ff1-f2ce-4a28-a1bd-d06538aad317.png" width="300" />
</p>

**Общая концепция солвера:** первый этап в автоматическом решении – создание групп. В данном случае, группы – это группы закрытых ячеек вокруг нумерованных ячеек. Происходит их реструктуризация. А именно проверки на их вхождения друг в друга и преобразование пересекающихся групп.
Основная черта этого автоматического решения – расчет вероятностей попадания на мину у каждой ячейки. По ходу решения происходят коррекции вероятностей, чтобы достичь стабильных значений вероятностей в каждой ячейке. При выборе следующей ячейки для открытия выбирается ячейка с наименьшей вероятностью попадания на мину. Также определяются «особые» ячейки, которые имеют наибольшую и наименьшую вероятность «заминирования». Те, что имеют наибольшую вероятность, автоматически отмечаются солвером флагом. А те, что имеют наименьшую, открываются в первую очередь.
Порой на поле получаются ситуации 50/50, где подсчет вероятностей не будет главным ориентиром.

