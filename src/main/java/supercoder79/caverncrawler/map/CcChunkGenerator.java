package supercoder79.caverncrawler.map;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import supercoder79.caverncrawler.map.carver.*;
import supercoder79.caverncrawler.map.gen.GeodeGen;
import xyz.nucleoid.plasmid.game.world.generator.GameChunkGenerator;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.ProbabilityConfig;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.carver.ConfiguredCarver;
import net.minecraft.world.gen.chunk.StructuresConfig;
import net.minecraft.world.gen.feature.EmeraldOreFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.OreFeatureConfig;

public class CcChunkGenerator extends GameChunkGenerator {
	private final long seed;
	private final List<ConfiguredCarver<ProbabilityConfig>> carvers = new ArrayList<>();
	public CcChunkGenerator(MinecraftServer server) {
		super(createBiomeSource(server, BiomeKeys.PLAINS), new StructuresConfig(Optional.empty(), Collections.emptyMap()));

		this.seed = new Random().nextLong();
		this.carvers.add(CaveCarver.INSTANCE);
		this.carvers.add(CaveRoomCarver.INSTANCE);
		this.carvers.add(HorizontalCarver.INSTANCE);
		this.carvers.add(BranchingRavineCarver.INSTANCE);
		this.carvers.add(VerticalCarver.INSTANCE);
	}

	@Override
	public void populateNoise(WorldAccess world, StructureAccessor structures, Chunk chunk) {
		ChunkPos pos = chunk.getPos();
		boolean isSpawn = Math.abs(pos.x) == 0 && Math.abs(pos.z) == 0;

		BlockPos.Mutable mutable = new BlockPos.Mutable();
		Random random = new Random();

		for (int x = 0; x < 16; x++) {
			mutable.setX(x);

		    for (int z = 0; z < 16; z++) {
				mutable.setZ(z);

				for (int y = 0; y < 128; y++) {
					mutable.setY(y);

					BlockState state = Blocks.STONE.getDefaultState();
					if (y == 0 || y == 127) {
						state = Blocks.BEDROCK.getDefaultState();
					}

					if (isSpawn && (y > 56 && y < 64)) {
						state = Blocks.AIR.getDefaultState();
					}

					// TODO: glowstone on sides of walls
					if (isSpawn && (y == 56 || y == 64)) {
						if (random.nextInt(5) == 0) {
							state = Blocks.GLOWSTONE.getDefaultState();
						}
					}

					chunk.setBlockState(mutable, state, false);
				}
		    }
		}
	}

	@Override
	public void carve(long seed, BiomeAccess access, Chunk chunk, GenerationStep.Carver step) {
		BiomeAccess biomeAccess = access.withSource(this.populationSource);
		ChunkRandom chunkRandom = new ChunkRandom();
		ChunkPos chunkPos = chunk.getPos();
		int chunkX = chunkPos.x;
		int chunkZ = chunkPos.z;
		BitSet bitSet = ((ProtoChunk)chunk).getOrCreateCarvingMask(step);

		for(int localChunkX = chunkX - 8; localChunkX <= chunkX + 8; ++localChunkX) {
			for(int localChunkZ = chunkZ - 8; localChunkZ <= chunkZ + 8; ++localChunkZ) {
				for (int i = 0; i < this.carvers.size(); i++) {
					chunkRandom.setCarverSeed(this.seed + i, localChunkX, localChunkZ);

					ConfiguredCarver<ProbabilityConfig> carver = this.carvers.get(i);
					if (carver.shouldCarve(chunkRandom, localChunkX, localChunkZ)) {
						carver.carve(chunk, biomeAccess::getBiome, chunkRandom, this.getSeaLevel(), localChunkX, localChunkZ, chunkX, chunkZ, bitSet);
					}
				}
			}
		}

	}

	@Override
	public void generateFeatures(ChunkRegion region, StructureAccessor structures) {
		int chunkX = region.getCenterChunkX();
		int chunkZ = region.getCenterChunkZ();

		Random random = new Random();

		if (chunkX == 0 && chunkZ == 0) {
			BlockPos.Mutable mutable = new BlockPos.Mutable();
			for (int x = 0; x < 16; x++) {
				mutable.setX(chunkX * 16 + x);

			    for (int z = 0; z < 16; z++) {
			    	mutable.setZ(chunkZ * 16 + z);

					for (int y = 48; y <= 56; y++) {
						mutable.setY(y);
						if (region.getBlockState(mutable).isAir()) {
							region.setBlockState(mutable, Blocks.STONE.getDefaultState(), 3);
						}
					}

					for (int y = 64; y <= 72; y++) {
						mutable.setY(y);
						if (region.getBlockState(mutable).isAir()) {
							region.setBlockState(mutable, Blocks.STONE.getDefaultState(), 3);
						}
					}
			    }
			}
		} else {
			if (chunkX == 0) {
				if (Math.abs(chunkZ) <= 3) {
					BlockPos.Mutable mutable = new BlockPos.Mutable();
					for (int x = 7; x <= 8; x++) {
						mutable.setX(chunkX * 16 + x);

						for (int z = 0; z < 16; z++) {
							mutable.setZ(chunkZ * 16 + z);

							for (int y = 57; y <= 60; y++) {
								mutable.setY(y);
								BlockState state = Blocks.AIR.getDefaultState();

								if (y == 60 && region.getBlockState(mutable).isOpaque()) {
									state = Blocks.STONE.getDefaultState();

									if (random.nextInt(6) == 0) {
										state = Blocks.GLOWSTONE.getDefaultState();
									}
								}

								region.setBlockState(mutable, state, 3);
							}
						}
					}
				}
			}

			if (chunkZ == 0) {
				if (Math.abs(chunkX) <= 3) {
					BlockPos.Mutable mutable = new BlockPos.Mutable();
					for (int x = 0; x < 16; x++) {
						mutable.setX(chunkX * 16 + x);

						for (int z = 7; z <= 8; z++) {
							mutable.setZ(chunkZ * 16 + z);

							for (int y = 57; y <= 60; y++) {
								mutable.setY(y);
								BlockState state = Blocks.AIR.getDefaultState();

								if (y == 60 && region.getBlockState(mutable).isOpaque()) {
									state = Blocks.STONE.getDefaultState();

									if (random.nextInt(6) == 0) {
										state = Blocks.GLOWSTONE.getDefaultState();
									}
								}

								region.setBlockState(mutable, state, 3);
							}
						}
					}
				}
			}
		}

		if (Math.abs(chunkX) <= 1 && Math.abs(chunkZ) <= 1) {
			for (int x = -1; x <= 16; x++) {
				for (int z = -1; z <= 16; z++) {
					if (x == -1 || x == 16 || z == -1 || z == 16) {
						for (int y = 57; y <= 60; y++) {
							BlockPos local = new BlockPos(x, y, z);
							if (region.getBlockState(local).isAir()) {
								region.setBlockState(local, Blocks.BARRIER.getDefaultState(), 3);
							}
						}
					}
				}
			}
		}

		if (Math.abs(chunkX) >= 1 && Math.abs(chunkZ) >= 1 && random.nextInt(3) == 0) {
			int x = random.nextInt(16) + (chunkX * 16);
			int y = random.nextInt(80) + 20;
			int z = random.nextInt(16) + (chunkZ * 16);
			GeodeGen.INSTANCE.generate(region, new BlockPos(x, y, z), random);
		}

		// TODO: refactor this hot mess
		for (int i = 0; i < 24; i++) {
			int x = random.nextInt(16) + (chunkX * 16);
			int y = random.nextInt(116) + 8;
			int z = random.nextInt(16) + (chunkZ * 16);
			Feature.ORE.generate(region, this, random, new BlockPos(x, y, z), new OreFeatureConfig(OreFeatureConfig.Rules.BASE_STONE_OVERWORLD, Blocks.COAL_ORE.getDefaultState(), 17));
		}

		for (int i = 0; i < 16; i++) {
			int x = random.nextInt(16) + (chunkX * 16);
			int y = random.nextInt(116) + 8;
			int z = random.nextInt(16) + (chunkZ * 16);
			Feature.ORE.generate(region, this, random, new BlockPos(x, y, z), new OreFeatureConfig(OreFeatureConfig.Rules.BASE_STONE_OVERWORLD, Blocks.IRON_ORE.getDefaultState(), 9));
		}

		for (int i = 0; i < 5; i++) {
			int x = random.nextInt(16) + (chunkX * 16);
			int y = random.nextInt(116) + 8;
			int z = random.nextInt(16) + (chunkZ * 16);
			Feature.ORE.generate(region, this, random, new BlockPos(x, y, z), new OreFeatureConfig(OreFeatureConfig.Rules.BASE_STONE_OVERWORLD, Blocks.GOLD_ORE.getDefaultState(), 9));
		}

		for (int i = 0; i < 10; i++) {
			int x = random.nextInt(16) + (chunkX * 16);
			int y = random.nextInt(116) + 8;
			int z = random.nextInt(16) + (chunkZ * 16);
			Feature.ORE.generate(region, this, random, new BlockPos(x, y, z), new OreFeatureConfig(OreFeatureConfig.Rules.BASE_STONE_OVERWORLD, Blocks.REDSTONE_ORE.getDefaultState(), 8));
		}

		for (int i = 0; i < 5; i++) {
			int x = random.nextInt(16) + (chunkX * 16);
			int y = random.nextInt(116) + 8;
			int z = random.nextInt(16) + (chunkZ * 16);
			Feature.ORE.generate(region, this, random, new BlockPos(x, y, z), new OreFeatureConfig(OreFeatureConfig.Rules.BASE_STONE_OVERWORLD, Blocks.LAPIS_ORE.getDefaultState(), 7));
		}

		for (int i = 0; i < 4 + random.nextInt(8); i++) {
			int x = random.nextInt(16) + (chunkX * 16);
			int y = random.nextInt(116) + 8;
			int z = random.nextInt(16) + (chunkZ * 16);
			Feature.EMERALD_ORE.generate(region, this, random, new BlockPos(x, y, z), new EmeraldOreFeatureConfig(Blocks.STONE.getDefaultState(), Blocks.EMERALD_ORE.getDefaultState()));
		}

		int x = random.nextInt(16) + (chunkX * 16);
		int y = random.nextInt(116) + 8;
		int z = random.nextInt(16) + (chunkZ * 16);
		Feature.ORE.generate(region, this, random, new BlockPos(x, y, z), new OreFeatureConfig(OreFeatureConfig.Rules.BASE_STONE_OVERWORLD, Blocks.DIAMOND_ORE.getDefaultState(), 8));
	}
}
