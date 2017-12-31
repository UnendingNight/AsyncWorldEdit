/*
 * AsyncWorldEdit a performance improvement plugin for Minecraft WorldEdit plugin.
 * Copyright (c) 2015, SBPrime <https://github.com/SBPrime/>
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
package org.primesoft.asyncworldedit.excommands.chunk;

import com.sk89q.worldedit.BlockVector2D;
import org.primesoft.asyncworldedit.api.worldedit.IAweEditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import java.util.HashMap;
import java.util.Set;
import org.primesoft.asyncworldedit.api.IWorld;
import org.primesoft.asyncworldedit.api.directChunk.IChangesetChunkData;
import org.primesoft.asyncworldedit.api.directChunk.IChunkData;
import org.primesoft.asyncworldedit.api.directChunk.ISerializedEntity;
import org.primesoft.asyncworldedit.api.directChunk.IWrappedChunk;
import org.primesoft.asyncworldedit.api.inner.IAsyncWorldEditCore;
import org.primesoft.asyncworldedit.api.playerManager.IPlayerEntry;
import org.primesoft.asyncworldedit.directChunk.ChangesetChunkExtent;
import org.primesoft.asyncworldedit.directChunk.DcUtils;
import org.primesoft.asyncworldedit.utils.Pair;
import org.primesoft.asyncworldedit.utils.PositionHelper;

/**
 *
 * @author SBPrime
 */
public class CloneChunkCommand extends DCMaskCommand {

    private final Region m_region;
    private final Vector m_location;
    private final World m_locationWorld;

    public CloneChunkCommand(Region region, Location location, World world,
            IAsyncWorldEditCore awe, Mask destinationMask, IPlayerEntry playerEntry) {
        super(awe, destinationMask, playerEntry);
        m_region = region;
        m_location = location.toVector();
        m_locationWorld = world;
    }

    @Override
    public String getName() {
        return "chunkClone";
    }

    @Override
    public Integer task(IAweEditSession editSesstion) throws WorldEditException {
        final Set<Vector2D> chunks = m_region.getChunks();
        final World weSource = m_region.getWorld();
        final IWorld wSource = m_weIntegrator.getWorld(weSource);
        final IWorld wDestination = m_weIntegrator.getWorld(m_locationWorld);
        final int clx = PositionHelper.positionToChunk(m_location.getX());
        final int clz = PositionHelper.positionToChunk(m_location.getZ());

        if (wDestination == null || wSource == null) {
            return 0;
        }

        int minCZ = Integer.MAX_VALUE;
        int minCX = Integer.MAX_VALUE;

        final HashMap<Vector2D, Pair<Boolean, LazyData>> chunksData = new HashMap<Vector2D, Pair<Boolean, LazyData>>();
        for (Vector2D c : chunks) {
            int z = c.getBlockZ();
            int x = c.getBlockX();

            if (z < minCZ) {
                minCZ = z;
            }
            if (x < minCX) {
                minCX = x;
            }

            chunksData.put(c, null);
        }

        final HashMap<Vector2D, IWrappedChunk> wrappedChunks = new HashMap<Vector2D, IWrappedChunk>();
        int changedBlocks = 0;

        for (final Vector2D cOrgPos : chunks) {
            final int cOrgX = cOrgPos.getBlockX();
            final int cOrgZ = cOrgPos.getBlockZ();
            final int cDstX = cOrgX - minCX + clx;
            final int cDstZ = cOrgZ - minCZ + clz;
            final Vector2D cDstPos = new BlockVector2D(cDstX, cDstZ);

            IWrappedChunk wcOrg, wcDst;

            synchronized (wrappedChunks) {
                if (wrappedChunks.containsKey(cOrgPos)) {
                    wcOrg = wrappedChunks.get(cOrgPos);
                } else {
                    wcOrg = DcUtils.wrapChunk(m_taskDispatcher, m_chunkApi,
                            weSource, wSource, getPlayer(), cOrgPos);
                    wrappedChunks.put(cOrgPos, wcOrg);
                }

                if (wrappedChunks.containsKey(cDstPos)) {
                    wcDst = wrappedChunks.get(cDstPos);
                } else {
                    wcDst = DcUtils.wrapChunk(m_taskDispatcher, m_chunkApi,
                            m_locationWorld, wDestination, getPlayer(), cDstPos);
                    wrappedChunks.put(cDstPos, wcDst);
                }
            }

            LazyData<IChunkData> data;
            synchronized (chunksData) {
                if (chunksData.containsKey(cDstPos)) {
                    Pair<Boolean, LazyData> entry = chunksData.get(cDstPos);

                    if (entry == null) {
                        LazyData tData = new LazyData();
                        chunksData.put(cDstPos, new Pair<Boolean, LazyData>(false, tData));
                        editSesstion.doCustomAction(new GetChunkData(wcDst, tData), false);
                    }
                }

                if (chunksData.containsKey(cOrgPos)) {
                    Pair<Boolean, LazyData> entry = chunksData.get(cOrgPos);

                    if (entry == null) {
                        chunksData.put(cOrgPos, new Pair<Boolean, LazyData>(true, null));

                        data = new LazyData();
                        editSesstion.doCustomAction(new GetChunkData(wcOrg, data), false);
                    } else {
                        data = entry.getX2();
                        chunksData.put(cOrgPos, new Pair<Boolean, LazyData>(true, null));
                    }
                } else {
                    //Should not happen
                    data = new LazyData();
                    editSesstion.doCustomAction(new GetChunkData(wcOrg, data), false);
                    chunksData.put(cOrgPos, new Pair<Boolean, LazyData>(true, null));
                }
            }

            final IChunkData source = data.get();
            final IChangesetChunkData destination = m_chunkApi.createLazyChunkData(wcDst);
            final ChangesetChunkExtent extent = new ChangesetChunkExtent(destination);
            final Vector zeroPos = PositionHelper.chunkToPosition(cDstPos, 0);

            maskSetExtent(extent);
            for (int x = 0; x < 16; x++) {
                final Vector xPos = zeroPos.add(x, 0, 0);
                for (int z = 0; z < 16; z++) {
                    final Vector zPos = xPos.add(0, 0, z);
                    for (int y = 0; y < 256; y++) {
                        final Vector yPos = zPos.add(0, y, 0);
                        if (maskTest(yPos)) {
                            destination.setBlock(x, y, z, source.getBlock(x, y, z));
                            changedBlocks++;
                        }
                    }
                }
            }
            for (ISerializedEntity e : destination.getEntity()) {
                if (maskTest(zeroPos.add(e.getPosition()))) {
                    destination.removeEntity(e);
                    changedBlocks++;
                }
            }
            for (ISerializedEntity e : source.getEntity()) {
                if (maskTest(zeroPos.add(e.getPosition()))) {
                    destination.addEntity(e);
                    changedBlocks++;
                }
            }
            maskSetExtent(null);

            editSesstion.doCustomAction(new SetChangesetChunkChange(wcDst, destination), false);
        }

        return changedBlocks;
    }
}
