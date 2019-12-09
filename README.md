# SixyExplainer

SixyExplainer is my modifications to Sudoku6x6Explainer to solve 6x6 SixySudokus.

SixySudoku is 6x6 Sudoku with an additional constraint, the numbers 1-6 must appear exactly once in each row, column, 2Rx3C block (white and grey areas) and 3Rx2C block (black outlined areas).

SixyExplainer does not solve SixySudoku puzzles where the 4th constraint is irregular jigsaw cages.

## Usage - GUI
java.exe -jar SixyExplainer.jar

![](/images/sample71.jpg)

## Usage - serate
java.exe -Xrs -Xmx500m -cp SixyExplainer.jar diuf.sudoku.test.serate --format="%g ED=%r/%p/%d" --input=puzzles.txt --output=output.txt

## Usage - hints
java.exe -Xrs -Xmx500m -cp SixyExplainer.jar diuf.sudoku.test.hints --input=puzzle.txt

