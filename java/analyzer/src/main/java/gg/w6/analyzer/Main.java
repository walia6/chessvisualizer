package gg.w6.analyzer;

import gg.w6.chesslib.model.*;
import gg.w6.chesslib.model.piece.*;
import gg.w6.chesslib.util.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Main {

    final static Position STARTING_POSITION;

    static {
        final PositionBuilder startingPositionBuilder = new PositionBuilder();

        // Add all starting pieces
        for (char file = 'a'; file <= 'h'; file++) {
            startingPositionBuilder.addPiece(new Pawn(Color.WHITE), Coordinate.valueOf(file + "2"));
            startingPositionBuilder.addPiece(new Pawn(Color.BLACK), Coordinate.valueOf(file + "7"));
        }

        startingPositionBuilder.addPiece(new Rook(Color.WHITE), Coordinate.valueOf("a1"));
        startingPositionBuilder.addPiece(new Rook(Color.WHITE), Coordinate.valueOf("h1"));
        startingPositionBuilder.addPiece(new Rook(Color.BLACK), Coordinate.valueOf("a8"));
        startingPositionBuilder.addPiece(new Rook(Color.BLACK), Coordinate.valueOf("h8"));

        startingPositionBuilder.addPiece(new Knight(Color.WHITE), Coordinate.valueOf("b1"));
        startingPositionBuilder.addPiece(new Knight(Color.WHITE), Coordinate.valueOf("g1"));
        startingPositionBuilder.addPiece(new Knight(Color.BLACK), Coordinate.valueOf("b8"));
        startingPositionBuilder.addPiece(new Knight(Color.BLACK), Coordinate.valueOf("g8"));

        startingPositionBuilder.addPiece(new Bishop(Color.WHITE), Coordinate.valueOf("c1"));
        startingPositionBuilder.addPiece(new Bishop(Color.WHITE), Coordinate.valueOf("f1"));
        startingPositionBuilder.addPiece(new Bishop(Color.BLACK), Coordinate.valueOf("c8"));
        startingPositionBuilder.addPiece(new Bishop(Color.BLACK), Coordinate.valueOf("f8"));

        startingPositionBuilder.addPiece(new Queen(Color.WHITE), Coordinate.valueOf("d1"));
        startingPositionBuilder.addPiece(new Queen(Color.BLACK), Coordinate.valueOf("d8"));

        startingPositionBuilder.addPiece(new King(Color.WHITE), Coordinate.valueOf("e1"));
        startingPositionBuilder.addPiece(new King(Color.BLACK), Coordinate.valueOf("e8"));

        STARTING_POSITION = startingPositionBuilder.toPosition();

    }

    public static void main(String[] args) throws Exception {

        final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        final Semaphore semaphore = new Semaphore(32); // Limit concurrency

        final File pgnFile = Paths.get(args[0]).toAbsolutePath().toFile();

        try (final PgnDatabaseSplitter splitter = new PgnDatabaseSplitter(pgnFile)) {
            for(final String gameString : splitter) {
                semaphore.acquire();
                executor.submit(() -> {
                    try {
                        final Game game = PgnParser.parse(gameString);

                        final String resultPrefixChar;

                        if (game.result().equals("1/2-1/2")) {
                            // game is a draw

                            resultPrefixChar = "d";

                            // Go to the last position
                            final Position position = getFinalPosition(game);

                            for (final Square square : position) {
                                final Piece piece = square.getPiece();

                                if (piece == null) {
                                    continue;
                                }

                                final String coordinateString = square.getCoordinate().toString();
                                final String pieceLetter = String.valueOf(Character.toUpperCase(piece.getLetter()));

                                System.out.println(resultPrefixChar + pieceLetter + coordinateString);
                            }
                        } else if (game.sanStrings().get(game.sanStrings().size() - 1).contains("#")) {
                            // game ends in checkmate

                            resultPrefixChar = "c";

                            // Go to the last position
                            final Position position = getFinalPosition(game);

                            final Coordinate kingToMoveCoordinate = getMatedKingCoordinate(position);

                            System.out.println(resultPrefixChar + "K" + kingToMoveCoordinate);

                            for (Coordinate attackerCoordinate : Positions.getTargetingCoordinates(kingToMoveCoordinate,
                                    position.getToMove() == Color.WHITE ? Color.BLACK : Color.WHITE, position)) {
                                final Piece attackerPiece = position.getSquare(attackerCoordinate).getPiece();
                                if (attackerPiece == null) {
                                    throw new IllegalStateException("Got a bad attacker coordinate. kingToMoveCoordinate="
                                            + kingToMoveCoordinate + " attackerCoordinate="+ attackerCoordinate
                                            + " fen=" + position.generateFEN());
                                }
                                System.out.println(resultPrefixChar + Character.toUpperCase(attackerPiece.getLetter()) + attackerCoordinate);
                            }
                        }
                    } finally {
                        System.out.println("game");
                        semaphore.release();
                    }


                });
            }
        }
        executor.shutdown();
        final boolean cleanTermination = executor.awaitTermination(5, TimeUnit.MINUTES);

        if (!cleanTermination) {
            throw new IllegalStateException("Termination timed out after 5 minutes.");
        }

        // System.out.println("Games: " + gamesCount);
    }

    private static Position getFinalPosition(final Game game) {
        Position position = STARTING_POSITION;
        for (final String sanString : game.sanStrings()) {
            final Move move = SanParser.parse(sanString, position);
            position = position.applyTo(move);
        }
        return position;
    }

    private static @NotNull Coordinate getMatedKingCoordinate(final Position position) {
        for (final Square square : position) {
            final Piece piece = square.getPiece();

            if (piece instanceof King && piece.getColor() == position.getToMove()) {
                return square.getCoordinate();
            }
        }
        throw new IllegalStateException("Couldn't find checkmated " + position.getToMove() + " king. FEN=" + position.generateFEN());
    }
}