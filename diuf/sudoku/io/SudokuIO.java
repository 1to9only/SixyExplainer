/*
 * Project: Sudoku Explainer
 * Copyright (C) 2006-2007 Nicolas Juillerat
 * Available under the terms of the Lesser General Public License (LGPL)
 */
package diuf.sudoku.io;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;
import java.util.List;

import diuf.sudoku.*;

/**
 * Static methods to load and store Sudokus from and to
 * files or the clipboard.
 * <p>
 * The support for formats is minimal and quick&dirty.
 * Only plain text formats are supported when reading:
 * <ul>
 * <li>A single line of 36 characters (all characters not in the
 * '1' - '6' range is considered as an empty cell).
 * <li>6 lines of 6 characters.
 * <li>Other multi-lines formats, with more than one character per cell,
 * or more than one line per row, or even with a few characters between
 * blocks might be supported, but there is no warranty. If a given format
 * works, and is not one of the first two above, you should consider you are lucky.
 * </ul>
 * <p>
 * When writing, the following format is used:
 * <ul>
 * <li>6 lines of 6 characters
 * <li>empty cells are represented by a '.'
 * </ul>
 */
public class SudokuIO {

    private static final int RES_OK = 2;
    private static final int RES_WARN = 1;
    private static final int RES_ERROR = 0;

    private static final String ERROR_MSG = "Unreadable Sudoku format";
    private static final String WARNING_MSG = "Warning: the Sudoku format was not recognized.\nThe Sudoku may not have been read correctly";

    private static int loadFromReader(Grid grid, Reader reader) throws IOException {
        List<String> lines = new ArrayList<String>();
        LineNumberReader lineReader = new LineNumberReader(reader);
        String line = lineReader.readLine();
        while (line != null) {
            lines.add(line);
            line = lineReader.readLine();
        }
        if (lines.size() > 1) {
            String allLines = "";
            String[] arrLines = new String[lines.size()];
            lines.toArray(arrLines);
            for (int i = 0; i < arrLines.length; i++)
                allLines += arrLines[i];
            int result = loadFromSingleLine(grid, allLines);
            return result;
        } else
        if (lines.size() == 1) {
            int result = loadFromSingleLine(grid, lines.get(0));
            return result;
        }
        return RES_ERROR;
    }

    private static int loadFromSingleLine(Grid grid, String line) {
        line += " "; // extra char
        int cellnum = 0;
        int cluenum = 0;
        int linelen = line.length();
        char ch = 0;

        int ispad = 0;
        int grpcnt = 0;
        int grpmax = 0;
        int cluecount = 0;
        while ( cluenum < linelen ) {
            ch = line.charAt(cluenum++);
            if (ch >= '1' && ch <= '9') { cluecount++; ispad = 0; grpcnt++; }
            else
            if (ch == '.' || ch == '0') { cluecount++; ispad = 0; grpcnt++; }
            else
            if ( ispad == 0 ) { ispad = 1; if ( grpcnt > grpmax ) { grpmax = grpcnt; } grpcnt = 0; }
        }

        cellnum = 0;
        cluenum = 0;

        if ( ( grpmax > 1 && grpmax <= 6 && cluecount > 36 ) ||
             cluecount >= 216 ) {   // pencilmarks  // clue clue - cell clues, if count=6 then next cell
            boolean prevchispad = true;             // clue pad  - next cell clues to follow
            boolean chispad = true;                 // pad  clue - cell clues follow
            int cluecounter = 0;                    // pad  pad  - consecutive pads are ignored

            for (int i = 0; i < 36; i++) {
                grid.setCellValue(i % 6, i / 6, 0);
                Cell cell = grid.getCell(i % 6, i / 6);
                cell.clearPotentialValues();
                cell.resetGiven();
            }
            while ( cellnum < 36 && cluenum < linelen ) {
                prevchispad = chispad;
                ch = line.charAt(cluenum++);
                chispad = true;
                if (ch >= '1' && ch <= '6') {
                    int value = ch - '0';
                    Cell cell = grid.getCell(cellnum % 6, cellnum / 6);
                    cell.addPotentialValue(value);
                    chispad = false;
                    cluecounter++;
                    if ( cluecounter == 6 ) {
                       cluecounter = 0;
                       cellnum++;
                    }
                }
                else
                if (ch == '.' || ch == '0') {
                    chispad = false;
                    cluecounter++;
                    if ( cluecounter == 6 ) {
                       cluecounter = 0;
                       cellnum++;
                    }
                }
                if ( chispad == true && prevchispad == false && cluecounter != 0 ) {
                   cluecounter = 0;
                   cellnum++;
                }
            }
            if ( cellnum == 36 ) {
                // fix naked singles
                for (int i = 0; i < 36; i++) {
                    Cell cell = grid.getCell(i % 6, i / 6);
                    if ( cell.getPotentialValues().cardinality() == 1 ) {
                        int singleclue = cell.getPotentialValues().nextSetBit(0);
                        boolean isnakedsingle = true;
                        for (Cell housecell : cell.getHouseCells()) {
                            if ( housecell.hasPotentialValue(singleclue) ) {
                                isnakedsingle = false;
                                break;
                            }
                        }
                        if ( isnakedsingle )
                        {
                            cell.setValue( singleclue);
                            cell.clearPotentialValues();
                            cell.setGiven();
                        }
                    }
                }
                grid.setPencilMarks();
            }
            return ( cellnum==36 ? RES_OK : RES_WARN);
        }

        if ( cluecount >= 36 ) { // sudoku
            while ( cellnum < 36 && cluenum < linelen ) {
                ch = line.charAt(cluenum++);
                if (ch >= '1' && ch <= '6') {
                    int value = ch - '0';
                    grid.setCellValue(cellnum % 6, cellnum / 6, value);
                    cellnum++;
                }
                else
                if (ch == '.' || ch == '0') {
                    cellnum++;
                }
            }
            return ( cellnum==36 ? RES_OK : RES_WARN);
        }

        return RES_ERROR;
    }

    private static void saveToWriter(Grid grid, Writer writer) throws IOException {
        for (int y = 0; y < 6; y++) {
            if ( y == 0 ) { writer.write("+--+--+--+\r\n"); }
            writer.write("|");
            for (int x = 0; x < 6; x++) {
                int value = grid.getCellValue(x, y);
                int ch = '.';
                if (value > 0) { ch = '0' + value; }
                writer.write(ch);
                if ( x == 1 || x == 3 ) { writer.write("|"); }
            }
            writer.write("|\r\n");
            if ( y%3  == 2 ) { writer.write("+--+--+--+\r\n"); }
        }
    }

    private static void savePencilMarksToWriter(Grid grid, Writer writer) throws IOException {
        int crd = 1;
        for (int y = 0; y < 6; y++) {
            for (int x = 0; x < 6; x++) {
                int n = grid.getCell(x, y).getPotentialValues().cardinality();
                if ( n > crd ) { crd = n; }
            }
        }
        for (int y = 0; y < 6; y++) {
            if ( y == 0 ) { writer.write("+-");
                for (int x = 0; x < 6; x++) {
                    for (int pad=0; pad<crd; pad++ ) { writer.write("-"); }
                    if ( x == 0 || x == 2 || x == 4 ) { writer.write("-"); }
                    if ( x == 1 || x == 3 ) { writer.write("-+-"); }
                }
                writer.write("-+\r\n");
            }
            writer.write("| ");
            for (int x = 0; x < 6; x++) { int cnt = 0;
                Cell cell = grid.getCell(x, y);
                int n = cell.getValue();
                for (int pv=1; pv<=6; pv++ ) {
                    if ( pv == n || cell.hasPotentialValue( pv) ) {
                        writer.write('0'+pv);
                        cnt++;
                    }
                }
                for (int pad=cnt; pad<crd; pad++ ) { writer.write(" "); }
                if ( x == 0 || x == 2 || x == 4 ) { writer.write(" "); }
                if ( x == 1 || x == 3 ) { writer.write(" | "); }
            }
            writer.write(" |\r\n");
            if ( y%3 == 2 ) { writer.write("+-");
                for (int x = 0; x < 6; x++) {
                    for (int pad=0; pad<crd; pad++ ) { writer.write("-"); }
                    if ( x == 0 || x == 2 || x == 4 ) { writer.write("-"); }
                    if ( x == 1 || x == 3 ) { writer.write("-+-"); }
                }
                writer.write("-+\r\n");
            }
        }
    }

    /**
     * Test whether a Sudoku can be loaded from the current
     * content of the clipboard.
     * @return whether a Sudoku can be loaded from the current
     * content of the clipboard
     */
    public static boolean isClipboardLoadable() {
        Grid grid = new Grid();
        return (loadFromClipboard(grid) == null);
    }

    public static ErrorMessage loadFromClipboard(Grid grid) {
        Transferable content =
            Toolkit.getDefaultToolkit().getSystemClipboard().getContents(grid);
        if (content == null)
            return new ErrorMessage("The clipboard is empty");
        Reader reader = null;
        try {
            DataFlavor flavor = new DataFlavor(String.class, "Plain text");
            reader = flavor.getReaderForText(content);
            int result = loadFromReader(grid, reader);
            if (result == RES_OK) // success
                return null;
            if (result == RES_WARN) // warning
                return new ErrorMessage(WARNING_MSG, false, (Object[])(new String[0]));
            else // error
                return new ErrorMessage(ERROR_MSG, true, (Object[])(new String[0]));
        } catch (IOException ex) {
            return new ErrorMessage("Error while copying:\n{0}", ex);
        } catch (UnsupportedFlavorException ex) {
            return new ErrorMessage("Unsupported data type");
        } finally {
            try {
                if (reader != null)
                    reader.close();
            } catch(Exception ex) {}
        }
    }

    public static void saveToClipboard(Grid grid) {
        StringWriter writer = new StringWriter();
        try {
            saveToWriter(grid, writer);
            StringSelection data = new StringSelection(writer.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(data, data);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void savePencilMarksToClipboard(Grid grid) {
        StringWriter writer = new StringWriter();
        try {
            savePencilMarksToWriter(grid, writer);
            StringSelection data = new StringSelection(writer.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(data, data);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static ErrorMessage loadFromFile(Grid grid, File file) {
        Reader reader = null;
        try {
            FileReader freader = new FileReader(file);
            reader = new BufferedReader(freader);
            int result = loadFromReader(grid, reader);
            if (result == RES_OK)
                return null;
            else if (result == RES_WARN)
                return new ErrorMessage(WARNING_MSG, false, (Object[])(new String[0]));
            else
                return new ErrorMessage(ERROR_MSG, true, (Object[])(new String[0]));
        } catch (FileNotFoundException ex) {
            return new ErrorMessage("File not found: {0}", file);
        } catch (IOException ex) {
            return new ErrorMessage("Error while reading file {0}:\n{1}", file, ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public static ErrorMessage saveToFile(Grid grid, File file) {
        Writer writer = null;
        try {
            FileWriter fwriter = new FileWriter(file);
            writer = new BufferedWriter(fwriter);
            saveToWriter(grid, writer);
            return null;
        } catch (IOException ex) {
            return new ErrorMessage("Error while writing file {0}:\n{1}", file, ex);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public static ErrorMessage savePencilMarksToFile(Grid grid, File file) {
        Writer writer = null;
        try {
            FileWriter fwriter = new FileWriter(file);
            writer = new BufferedWriter(fwriter);
            savePencilMarksToWriter(grid, writer);
            return null;
        } catch (IOException ex) {
            return new ErrorMessage("Error while writing file {0}:\n{1}", file, ex);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

}
