/*
 * AsyncWorldEdit a performance improvement plugin for Minecraft WorldEdit plugin.
 * Copyright (c) 2014, SBPrime <https://github.com/SBPrime/>
 * Copyright (c) AsyncWorldEdit contributors
 *
 * All rights reserved.
 *
 * Redistribution in source, use in source and binary forms, with or without
 * modification, are permitted free of charge provided that the following 
 * conditions are met:
 *
 * 1.  Redistributions of source code must retain the above copyright notice, this
 *     list of conditions and the following disclaimer.
 * 2.  Redistributions of source code, with or without modification, in any form
 *     other then free of charge is not allowed,
 * 3.  Redistributions of source code, with tools and/or scripts used to build the 
 *     software is not allowed,
 * 4.  Redistributions of source code, with information on how to compile the software
 *     is not allowed,
 * 5.  Providing information of any sort (excluding information from the software page)
 *     on how to compile the software is not allowed,
 * 6.  You are allowed to build the software for your personal use,
 * 7.  You are allowed to build the software using a non public build server,
 * 8.  Redistributions in binary form in not allowed.
 * 9.  The original author is allowed to redistrubute the software in bnary form.
 * 10. Any derived work based on or containing parts of this software must reproduce
 *     the above copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided with the
 *     derived work.
 * 11. The original author of the software is allowed to change the license
 *     terms or the entire license of the software as he sees fit.
 * 12. The original author of the software is allowed to sublicense the software
 *     or its parts using any license terms he sees fit.
 * 13. By contributing to this project you agree that your contribution falls under this
 *     license.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.primesoft.asyncworldedit.commands;

import org.primesoft.asyncworldedit.core.Help;
import org.primesoft.asyncworldedit.*;
import org.primesoft.asyncworldedit.api.MessageSystem;
import org.primesoft.asyncworldedit.api.configuration.IPermissionGroup;
import org.primesoft.asyncworldedit.api.inner.IAsyncWorldEditCore;
import org.primesoft.asyncworldedit.api.playerManager.IPlayerEntry;
import org.primesoft.asyncworldedit.api.playerManager.IPlayerManager;
import org.primesoft.asyncworldedit.permissions.Permission;
import org.primesoft.asyncworldedit.strings.MessageType;

/**
 *
 * @author SBPrime
 */
public class MessagesCommand {

    public static void Execte(IAsyncWorldEditCore sender, IPlayerEntry player, String[] args) {
        if (args.length < 3 || args.length > 4) {
            Help.ShowHelp(player, Commands.COMMAND_MESSAGES);
            return;
        }

        IPlayerManager manager = sender.getPlayerManager();
        IPlayerEntry wrapper;
        boolean mode;

        int modePos;
        int systemPos;

        if (args.length == 3) {
            if (!player.isInGame()) {
                player.say(MessageType.INGAME.format());
                return;
            }
            if (!player.isAllowed(Permission.MESSAGES_CHANGE)) {
                player.say(MessageType.NO_PERMS.format());
                return;
            }

            wrapper = player;
            systemPos = 1;
            modePos = 2;
        } else {
            String arg = args[1];
            if (arg.startsWith("u:")) {
                if (!player.isAllowed(Permission.MESSAGES_CHANGE_OTHER)) {
                    player.say(MessageType.NO_PERMS.format());
                    return;
                }

                String name = arg.substring(2);
                wrapper = sender.getPlayerManager().getPlayer(name);
                if (!wrapper.isPlayer()) {
                    player.say(MessageType.PLAYER_NOT_FOUND.format());
                    return;
                }

                systemPos = 2;
                modePos = 3;
            } else {
                Help.ShowHelp(player, Commands.COMMAND_MESSAGES);
                return;
            }
        }

        String arg = args[modePos];
        if (arg.equalsIgnoreCase("on")) {
            mode = true;
        } else if (arg.equalsIgnoreCase("off")) {
            mode = false;
        } else {
            Help.ShowHelp(player, Commands.COMMAND_MESSAGES);
            return;
        }

        MessageSystem system = null;
        String systemName = args[systemPos].toLowerCase();
        for (MessageSystem ms : MessageSystem.values()) {
            if (ms.getName().equalsIgnoreCase(systemName)) {
                system = ms;
                break;
            }
        }

        if (system == null) {
            player.say(MessageType.CMD_MESSAGE_UNKNOWN.format(systemName));
            Help.ShowHelp(player, Commands.COMMAND_MESSAGES);
            return;
        }

        if (mode) {
            IPermissionGroup permsGroup = wrapper.getPermissionGroup();

            boolean checkRequired;
            switch (system) {
                case TALKATIVE:
                    checkRequired = !permsGroup.isTalkative();
                    break;
                case CHAT:
                    checkRequired = !permsGroup.isChatProgressEnabled();
                    break;
                case BAR:
                    checkRequired = !permsGroup.isBarApiProgressEnabled();
                    break;
                default:
                    checkRequired = false;
                    break;
            }

            if (checkRequired && !player.isAllowed(Permission.MESSAGES_CHANGE_OVERRIDE)) {
                player.say(MessageType.NO_PERMS.format());
                return;
            }
        }

        wrapper.setMessaging(system, mode);
        player.say(MessageType.CMD_MESSAGE_OK.format(system.getName().toUpperCase(),
                mode ? MessageType.CMD_MESSAGE_ON.format() : MessageType.CMD_MESSAGE_OFF.format()));
    }
}
