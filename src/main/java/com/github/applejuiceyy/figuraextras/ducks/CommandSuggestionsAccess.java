package com.github.applejuiceyy.figuraextras.ducks;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.gui.Font;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

public interface CommandSuggestionsAccess {
    void figuraExtras$setUseFiguraSuggester(boolean use);

    boolean figuraExtras$shouldShowFiguraBadges();

    Font figuraExtras$getFont();

    interface SuggestionBehaviour {
        static SuggestionBehaviour parse(LuaValue value, String input) throws LuaError {
            LuaValue verifying;

            if (!(verifying = value).istable()) {
                throw new LuaError("Second return value from autocomplete event must be a Table");
            }
            LuaTable table = verifying.checktable();

            if (!(verifying = table.rawget("result")).isstring()) {
                throw new LuaError("Table from autocomplete event expected a String in field \"result\"");
            }

            String s = verifying.checkjstring();
            switch (s) {
                case "suggest" -> {
                    if (!(verifying = table.rawget("suggestions")).istable()) {
                        throw new LuaError("Table from autocomplete event expected a Table in field \"suggestions\"");
                    }
                    LuaTable suggestions = verifying.checktable();

                    if (!(verifying = table.rawget("position")).isint()) {
                        throw new LuaError("Table from autocomplete event expected an Integer in field \"position\"");
                    }
                    int position = verifying.checkint();

                    if (position < 1) {
                        throw new LuaError("Provided position in autocomplete event must not be smaller than 1");
                    }
                    if (position > input.length() + 1) {
                        throw new LuaError("Provided position in autocomplete event cannot surpass length of input String");
                    }

                    SuggestionsBuilder builder = new SuggestionsBuilder(input, position - 1);
                    for (int i = 1; suggestions.get(i) != LuaValue.NIL; i++) {
                        verifying = suggestions.get(i);
                        String tooltip, name;

                        if (verifying.isstring()) {
                            name = verifying.tojstring();
                            tooltip = null;

                        } else if (verifying.istable()) {
                            LuaTable suggestion = verifying.checktable();

                            if (!(verifying = suggestion.rawget("name")).isstring()) {
                                throw new LuaError("Tables inside array \"suggestions\" from the Table from the autocomplete expected a String in field \"name\"");
                            }

                            name = verifying.checkjstring();
                            verifying = suggestion.rawget("tooltip");
                            if (verifying.isnil()) {
                                tooltip = null;
                            } else if (verifying.isstring()) {
                                tooltip = verifying.checkjstring();
                            } else {
                                throw new LuaError("Tables inside array \"suggestions\" from the Table from the autocomplete expected a String or nil in field \"tooltip\"");
                            }
                        } else {
                            throw new LuaError("Table \"suggestions\" from the Table from the autocomplete event was expected to be an array of Strings or Tables, but caught %s in position %d"
                                    .formatted(verifying.typename(), i)
                            );
                        }

                        if (tooltip == null) {
                            builder.suggest(name);
                        } else {
                            builder.suggest(name, new LiteralMessage(tooltip));
                        }
                    }
                    return new CommandSuggestionsAccess.AcceptBehaviour(builder.build());
                }
                case "usage" -> {
                    if (!(verifying = table.rawget("usage")).isstring()) {
                        throw new LuaError("Table from autocomplete event expected a String in field \"usage\"");
                    }
                    String usage = verifying.tojstring();
                    if (!(verifying = table.rawget("position")).isint()) {
                        throw new LuaError("Table from autocomplete event expected an Integer in field \"position\"");
                    }
                    return new CommandSuggestionsAccess.HintBehaviour(usage, verifying.checkint() - 1);
                }
                case "error" -> {
                    if (!(verifying = table.rawget("message")).isstring()) {
                        throw new LuaError("Table from autocomplete event expected a String in field \"message\"");
                    }
                    return new CommandSuggestionsAccess.RejectBehaviour(verifying.tojstring());
                }
                default ->
                        throw new LuaError("The field \"result\" from the Table from autocomplete event must be of either \"suggest\", \"usage\" or \"error\"; But received \"%s\" instead".formatted(s));
            }
        }
    }

    record AcceptBehaviour(Suggestions suggest) implements SuggestionBehaviour {
    }

    record HintBehaviour(String hint, int pos) implements SuggestionBehaviour {
    }

    record RejectBehaviour(String err) implements SuggestionBehaviour {
    }
}
