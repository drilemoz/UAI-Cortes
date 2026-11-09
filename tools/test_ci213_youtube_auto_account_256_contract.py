#!/usr/bin/env python3
"""Contrato v0.1.256: import CI 213, migração OAuth consentida e verificações contra regressão.
Não substitui build Android nem teste de duas contas Google no aparelho.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / 'app/src/main/java/br/com/uaicortes'

class YouTubeAutoAccount256(unittest.TestCase):
    def test_ci213_horizontal_scroll_compose_import(self):
        screen = (SRC/'StandaloneTextToVoiceScreen.kt').read_text()
        assert 'import androidx.compose.foundation.horizontalScroll' in screen
        assert '.horizontalScroll(rememberScrollState())' in screen

    def test_one_time_auto_association_on_explicit_refresh(self):
        screen = (SRC/'SocialScreens.kt').read_text()
        auth = (SRC/'YouTubeAuthorizationUi.kt').read_text()
        assert 'authorizeYouTube.forUnlinkedChannel' in screen
        assert 'pendingChannelQueue = distinct' in screen
        assert 'withVerifiedToken(token, needsAssociation = true)' in screen
        assert 'if (channel.id != channelId)' in screen
        assert 'YouTubeGoogleProfileApi.verifiedEmail(token)' in screen
        assert 'require(existing.remoteChannelId == channelId)' in screen
        assert 'it.copy(googleAccountEmail = discoveredEmail)' in screen
        assert 'store.save(updated)' in screen
        assert 'else authorizeYouTube.forChannel(email)' in screen
        assert 'start(true, YouTubePermission.IDENTIFY, null, onToken)' in auth
        assert 'builder.setAccount(Account(accountEmail, "com.google"))' in auth
        assert 'AuthorizationRequest.Prompt.SELECT_ACCOUNT' in auth
        assert 'else if (chooseAccount)' in auth

    def test_oauth_scopes_and_verified_email(self):
        auth = (SRC/'YouTubeAuthorizationUi.kt').read_text()
        profile = (SRC/'YouTubeGoogleProfileApi.kt').read_text()
        assert 'https://www.googleapis.com/auth/userinfo.email' in auth
        assert 'YouTubePermission.IDENTIFY ->' in auth
        assert 'YouTubePermission.READ -> emptyList()' in auth
        assert 'YouTubePermission.PUBLISH ->' in auth
        assert 'https://www.googleapis.com/auth/youtube.upload' in auth
        assert 'https://www.googleapis.com/oauth2/v3/userinfo' in profile
        assert 'json.optBoolean("email_verified", false)' in profile
        assert 'YouTubeGoogleAccountEmail.valid(email)' in profile
        assert 'connection.disconnect()' in profile
        assert 'requestOfflineAccess' not in auth
        assert 'setRequestProperty("Authorization", "Bearer $accessToken")' in profile
        assert 'accessToken' not in (SRC/'SocialStore.kt').read_text()

    def test_onboarding_and_existing_publication_preserved(self):
        screen = (SRC/'SocialScreens.kt').read_text()
        publication = (SRC/'SocialPublishScreen.kt').read_text()
        assert 'newChannelGoogleEmail = email' in screen
        assert 'it.copy(googleAccountEmail = newChannelGoogleEmail)' in screen
        assert '.copy(googleAccountEmail = bindChannelGoogleEmail)' in screen
        assert 'authorizeYouTube.forUpload(account.googleAccountEmail.ifBlank { null })' in publication
        assert 'require(channel.id == account.remoteChannelId)' in publication
        assert 'GoogleAccountProfileApi' not in publication

if __name__ == '__main__':
    unittest.main()
