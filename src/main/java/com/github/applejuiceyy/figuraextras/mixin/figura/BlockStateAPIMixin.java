package com.github.applejuiceyy.figuraextras.mixin.figura;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.api.world.BlockStateAPI;
import org.figuramc.figura.math.vector.FiguraVec2;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.*;

@Mixin(BlockStateAPI.class)
public class BlockStateAPIMixin {
    @Shadow
    @Final
    public BlockState blockState;

    @Shadow
    private BlockPos pos;
    @Unique
    private final Map<RenderShape, Map<String, ArrayList<HashMap<String, Object>>>> cache = new HashMap<>();

    @LuaWhitelist
    public Vec3 getRenderOffset() {
        return blockState.getOffset(Minecraft.getInstance().level, pos);
    }

    @LuaWhitelist
    public Map<String, ArrayList<HashMap<String, Object>>> getQuads() {
        RenderShape renderShape = blockState.getRenderShape();
        if (renderShape != RenderShape.MODEL)
            return Collections.emptyMap();

        if (cache.containsKey(renderShape)) {
            return cache.get(renderShape);
        }
        HashMap<String, ArrayList<HashMap<String, Object>>> map = new HashMap<>();

        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        BakedModel bakedModel = blockRenderer.getBlockModel(blockState);
        RandomSource randomSource = RandomSource.create();
        long seed = 42L;

        for (Direction direction : Direction.values())
            map.put(direction.name(), collectQuads(blockState, direction, randomSource, bakedModel, seed));
        map.put("NONE", collectQuads(blockState, null, randomSource, bakedModel, seed));
        cache.put(renderShape, map);
        return map;
    }

    @Unique
    public ArrayList<HashMap<String, Object>> collectQuads(BlockState blockState, Direction direction, RandomSource randomSource, BakedModel bakedModel, long seed) {
        randomSource.setSeed(seed);
        List<BakedQuad> quads = bakedModel.getQuads(blockState, direction, randomSource);
        ArrayList<HashMap<String, Object>> transformed = new ArrayList<>();

        for (BakedQuad quad : quads) {
            HashMap<String, Object> map = new HashMap<>();

            int[] vertexData = quad.getVertices();
            ArrayList<HashMap<String, Object>> vertices = new ArrayList<>();
            for (int l = 0; l < 4; ++l) {
                HashMap<String, Object> transformedVertexData = new HashMap<>();

                float x = Float.intBitsToFloat(vertexData[l * 8]);
                float y = Float.intBitsToFloat(vertexData[l * 8 + 1]);
                float z = Float.intBitsToFloat(vertexData[l * 8 + 2]);

                float u = Float.intBitsToFloat(vertexData[l * 8 + 4]);
                float v = Float.intBitsToFloat(vertexData[l * 8 + 5]);

                transformedVertexData.put("pos", FiguraVec3.of(x, y, z));
                transformedVertexData.put("uv", FiguraVec2.of(u, v));

                vertices.add(transformedVertexData);
            }

            map.put("vertices", vertices);

            map.put("texture", quad.getSprite().atlasLocation().toString());

            transformed.add(map);
        }


        return transformed;
    }
}
