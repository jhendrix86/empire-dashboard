package com.empire.server.orchestration.stages

import com.empire.dashboard.data.SelectedNiche
import com.empire.server.orchestration.RunManifest
import com.empire.server.orchestration.StageOutcome
import com.empire.server.orchestration.artifacts.DeliverableFile
import com.empire.server.orchestration.artifacts.DeliverableManifest
import com.empire.server.orchestration.artifacts.EnterpriseBlueprint
import com.empire.server.orchestration.artifacts.ResearchBrief
import com.empire.server.etsy.EtsyListing
import com.empire.server.orchestration.writeArtifact
import com.empire.server.shopify.ShopifyProduct
import com.empire.server.storage.RunRepository
import com.empire.server.testutil.FakeEtsyClient
import com.empire.server.testutil.FakeLlmClient
import com.empire.server.testutil.FakeShopifyClient
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ShippingStageTest {
    private fun newRepo(): RunRepository = RunRepository(Files.createTempDirectory("empire-test").toFile())

    @Test
    fun `bundles the product deliverable, lead magnet, and launch instructions`() = runBlocking {
        val repo = newRepo()
        val runId = "run-1"
        repo.save(RunManifest(runId = runId, createdAt = "now"))

        writeArtifact(
            repo, runId, "research-brief.json", ResearchBrief.serializer(),
            ResearchBrief(
                niche = SelectedNiche(niche = "Pet care", subNiche = "Senior Dog Nutrition"),
                audienceProfile = "profile",
                coreProblemDetail = "problem",
                competitiveLandscape = "landscape",
                legalComplianceNotes = "notes",
                monetizationAngle = "angle",
                recommendedProductFormat = "ebook"
            )
        )
        writeArtifact(
            repo, runId, "blueprint.json", EnterpriseBlueprint.serializer(),
            EnterpriseBlueprint(
                productOutline = "outline",
                brandVoiceGuide = "voice",
                visualDirection = "visual",
                pricing = "19.00",
                leadMagnetConcept = "lead",
                funnelDesign = "funnel",
                outputFormats = listOf("ebook"),
                platformTargets = listOf("gumroad")
            )
        )
        val deliverableManifest = DeliverableManifest(
            files = listOf(DeliverableFile(format = "ebook", fileName = "product-ebook.md", description = "main")),
            leadMagnetFileName = "lead-magnet.md"
        )
        writeArtifact(repo, runId, "deliverable-manifest.json", DeliverableManifest.serializer(), deliverableManifest)

        val deliverablesDir = File(repo.runDir(runId), "deliverables").apply { mkdirs() }
        File(deliverablesDir, "product-ebook.md").writeText("the ebook")
        File(deliverablesDir, "lead-magnet.md").writeText("the lead magnet")

        val shopify = FakeShopifyClient()
        val etsy = FakeEtsyClient()
        val result = ShippingStage(FakeLlmClient { _, _ -> "launch instructions" }, repo, shopify, etsy)
            .run(runId, repo.load(runId)!!)

        assertIs<StageOutcome.Continue>(result.outcome)
        val bundle = repo.load(runId)?.bundle
        assertEquals(true, bundle?.bundleExists)
        assertEquals("Senior Dog Nutrition", bundle?.productName)
        assertEquals(
            setOf("product-ebook.md", "lead-magnet.md", "instructions.md"),
            bundle?.copiedFiles?.toSet()
        )
        assertEquals(null, bundle?.shopifyProductUrl)
        assertEquals(null, bundle?.etsyListingUrl)

        val instructionsFile = File(repo.runDir(runId), "bundle/instructions.md")
        assertTrue(instructionsFile.exists())
        assertEquals("launch instructions", instructionsFile.readText())
    }

    @Test
    fun `creates a draft Shopify listing priced from the blueprint and saves its admin url`() = runBlocking {
        val repo = newRepo()
        val runId = "run-2"
        repo.save(RunManifest(runId = runId, createdAt = "now"))

        writeArtifact(
            repo, runId, "research-brief.json", ResearchBrief.serializer(),
            ResearchBrief(
                niche = SelectedNiche(niche = "Pet care", subNiche = "Senior Dog Nutrition"),
                audienceProfile = "profile",
                coreProblemDetail = "problem",
                competitiveLandscape = "landscape",
                legalComplianceNotes = "notes",
                monetizationAngle = "angle",
                recommendedProductFormat = "ebook"
            )
        )
        writeArtifact(
            repo, runId, "blueprint.json", EnterpriseBlueprint.serializer(),
            EnterpriseBlueprint(
                productOutline = "outline",
                brandVoiceGuide = "voice",
                visualDirection = "visual",
                pricing = "$29 one-time purchase",
                leadMagnetConcept = "lead",
                funnelDesign = "funnel",
                outputFormats = listOf("ebook"),
                platformTargets = listOf("gumroad")
            )
        )
        val deliverableManifest = DeliverableManifest(
            files = listOf(DeliverableFile(format = "ebook", fileName = "product-ebook.md", description = "main")),
            leadMagnetFileName = "lead-magnet.md"
        )
        writeArtifact(repo, runId, "deliverable-manifest.json", DeliverableManifest.serializer(), deliverableManifest)

        val deliverablesDir = File(repo.runDir(runId), "deliverables").apply { mkdirs() }
        File(deliverablesDir, "product-ebook.md").writeText("the ebook")
        File(deliverablesDir, "lead-magnet.md").writeText("the lead magnet")

        val shopify = FakeShopifyClient(ShopifyProduct(id = 42, adminUrl = "https://store.myshopify.com/admin/products/42"))
        val etsy = FakeEtsyClient()
        val result = ShippingStage(FakeLlmClient { _, _ -> "launch instructions" }, repo, shopify, etsy)
            .run(runId, repo.load(runId)!!)

        assertIs<StageOutcome.Continue>(result.outcome)
        assertEquals("https://store.myshopify.com/admin/products/42", repo.load(runId)?.bundle?.shopifyProductUrl)
        assertEquals(1, shopify.calls.size)
        assertEquals("Senior Dog Nutrition", shopify.calls.single().title)
        assertEquals(29.0, shopify.calls.single().priceUsd)
    }

    @Test
    fun `creates a draft Etsy listing priced from the blueprint and saves its url`() = runBlocking {
        val repo = newRepo()
        val runId = "run-3"
        repo.save(RunManifest(runId = runId, createdAt = "now"))

        writeArtifact(
            repo, runId, "research-brief.json", ResearchBrief.serializer(),
            ResearchBrief(
                niche = SelectedNiche(niche = "Pet care", subNiche = "Senior Dog Nutrition"),
                audienceProfile = "profile",
                coreProblemDetail = "problem",
                competitiveLandscape = "landscape",
                legalComplianceNotes = "notes",
                monetizationAngle = "angle",
                recommendedProductFormat = "ebook"
            )
        )
        writeArtifact(
            repo, runId, "blueprint.json", EnterpriseBlueprint.serializer(),
            EnterpriseBlueprint(
                productOutline = "outline",
                brandVoiceGuide = "voice",
                visualDirection = "visual",
                pricing = "$29 one-time purchase",
                leadMagnetConcept = "lead",
                funnelDesign = "funnel",
                outputFormats = listOf("ebook"),
                platformTargets = listOf("etsy")
            )
        )
        val deliverableManifest = DeliverableManifest(
            files = listOf(DeliverableFile(format = "ebook", fileName = "product-ebook.md", description = "main")),
            leadMagnetFileName = "lead-magnet.md"
        )
        writeArtifact(repo, runId, "deliverable-manifest.json", DeliverableManifest.serializer(), deliverableManifest)

        val deliverablesDir = File(repo.runDir(runId), "deliverables").apply { mkdirs() }
        File(deliverablesDir, "product-ebook.md").writeText("the ebook")
        File(deliverablesDir, "lead-magnet.md").writeText("the lead magnet")

        val shopify = FakeShopifyClient()
        val etsy = FakeEtsyClient(EtsyListing(listingId = 987, url = "https://www.etsy.com/listing/987"))
        val result = ShippingStage(FakeLlmClient { _, _ -> "launch instructions" }, repo, shopify, etsy)
            .run(runId, repo.load(runId)!!)

        assertIs<StageOutcome.Continue>(result.outcome)
        assertEquals("https://www.etsy.com/listing/987", repo.load(runId)?.bundle?.etsyListingUrl)
        assertEquals(1, etsy.calls.size)
        assertEquals("Senior Dog Nutrition", etsy.calls.single().title)
        assertEquals(29.0, etsy.calls.single().priceUsd)
    }
}
