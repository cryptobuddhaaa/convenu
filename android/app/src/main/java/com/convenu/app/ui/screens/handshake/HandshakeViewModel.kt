package com.convenu.app.ui.screens.handshake

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.convenu.app.data.model.HandshakeDto
import com.convenu.app.data.repository.HandshakeRepository
import com.convenu.app.util.MwaWalletManager
import com.convenu.app.util.WalletResult
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class HandshakeListUiState(
    val isLoading: Boolean = true,
    val handshakes: List<HandshakeDto> = emptyList(),
    val error: String? = null,
)

data class HandshakeActionState(
    val isProcessing: Boolean = false,
    val currentAction: String? = null,
    val error: String? = null,
    val success: String? = null,
)

@HiltViewModel
class HandshakeViewModel @Inject constructor(
    private val handshakeRepository: HandshakeRepository,
    private val walletManager: MwaWalletManager,
) : ViewModel() {

    private val _listState = MutableStateFlow(HandshakeListUiState())
    val listState: StateFlow<HandshakeListUiState> = _listState.asStateFlow()

    private val _actionState = MutableStateFlow(HandshakeActionState())
    val actionState: StateFlow<HandshakeActionState> = _actionState.asStateFlow()

    init {
        loadHandshakes()
    }

    fun loadHandshakes() {
        viewModelScope.launch {
            _listState.value = _listState.value.copy(isLoading = true, error = null)

            handshakeRepository.getPending().onSuccess { handshakes ->
                _listState.value = _listState.value.copy(
                    isLoading = false,
                    handshakes = handshakes,
                )
            }.onFailure {
                _listState.value = _listState.value.copy(
                    isLoading = false,
                    error = it.message,
                )
            }
        }
    }

    fun initiateHandshake(
        contactId: String,
        walletAddress: String,
        sender: ActivityResultSender,
    ) {
        if (_actionState.value.isProcessing) return

        viewModelScope.launch {
            _actionState.value = HandshakeActionState(isProcessing = true, currentAction = "Initiating...")

            // Step 1: Initiate
            val initiateResult = handshakeRepository.initiate(contactId, walletAddress)
            val initiateResponse = initiateResult.getOrElse {
                _actionState.value = HandshakeActionState(error = it.message)
                return@launch
            }

            // Step 2: Decode base64 transaction and sign
            _actionState.value = _actionState.value.copy(currentAction = "Sign transaction in wallet...")
            val txBytes = Base64.decode(initiateResponse.transaction, Base64.DEFAULT)

            when (val signResult = walletManager.signTransactions(sender, arrayOf(txBytes))) {
                is WalletResult.Success -> {
                    val signedTxBase64 = Base64.encodeToString(signResult.data[0], Base64.NO_WRAP)

                    // Step 3: Confirm
                    _actionState.value = _actionState.value.copy(currentAction = "Confirming...")
                    val confirmResult = handshakeRepository.confirmTransaction(
                        handshakeId = initiateResponse.handshakeId,
                        signedTransaction = signedTxBase64,
                        side = "initiator",
                    )

                    confirmResult.onSuccess {
                        _actionState.value = HandshakeActionState(
                            success = "Handshake initiated! Waiting for ${initiateResponse.contactName} to claim.",
                        )
                        loadHandshakes()
                    }.onFailure {
                        _actionState.value = HandshakeActionState(error = it.message)
                    }
                }

                is WalletResult.Cancelled -> {
                    _actionState.value = HandshakeActionState(error = "Transaction signing cancelled. You can retry.")
                }

                is WalletResult.NoWallet -> {
                    _actionState.value = HandshakeActionState(error = "No Solana wallet found.")
                }

                is WalletResult.Error -> {
                    _actionState.value = HandshakeActionState(error = signResult.message)
                }
            }
        }
    }

    fun claimHandshake(
        handshakeId: String,
        walletAddress: String,
        sender: ActivityResultSender,
    ) {
        if (_actionState.value.isProcessing) return

        viewModelScope.launch {
            _actionState.value = HandshakeActionState(isProcessing = true, currentAction = "Claiming...")

            // Step 1: Claim
            val claimResult = handshakeRepository.claim(handshakeId, walletAddress)
            val claimResponse = claimResult.getOrElse {
                _actionState.value = HandshakeActionState(error = it.message)
                return@launch
            }

            // Step 2: Sign transaction
            _actionState.value = _actionState.value.copy(currentAction = "Sign transaction in wallet...")
            val txBytes = Base64.decode(claimResponse.transaction, Base64.DEFAULT)

            when (val signResult = walletManager.signTransactions(sender, arrayOf(txBytes))) {
                is WalletResult.Success -> {
                    val signedTxBase64 = Base64.encodeToString(signResult.data[0], Base64.NO_WRAP)

                    // Step 3: Confirm
                    _actionState.value = _actionState.value.copy(currentAction = "Confirming...")
                    val confirmResult = handshakeRepository.confirmTransaction(
                        handshakeId = handshakeId,
                        signedTransaction = signedTxBase64,
                        side = "receiver",
                    )

                    confirmResult.onSuccess { response ->
                        val message = if (response.bothPaid) {
                            "Both sides confirmed! Ready to mint."
                        } else {
                            "Claimed! Waiting for initiator to confirm."
                        }
                        _actionState.value = HandshakeActionState(success = message)
                        loadHandshakes()
                    }.onFailure {
                        _actionState.value = HandshakeActionState(error = it.message)
                    }
                }

                is WalletResult.Cancelled -> {
                    _actionState.value = HandshakeActionState(error = "Transaction signing cancelled. You can retry.")
                }

                is WalletResult.NoWallet -> {
                    _actionState.value = HandshakeActionState(error = "No Solana wallet found.")
                }

                is WalletResult.Error -> {
                    _actionState.value = HandshakeActionState(error = signResult.message)
                }
            }
        }
    }

    fun mintHandshake(handshakeId: String) {
        if (_actionState.value.isProcessing) return

        viewModelScope.launch {
            _actionState.value = HandshakeActionState(isProcessing = true, currentAction = "Minting...")

            handshakeRepository.mint(handshakeId).onSuccess { response ->
                _actionState.value = HandshakeActionState(
                    success = "Minted! ${response.pointsAwarded} points awarded.",
                )
                loadHandshakes()
            }.onFailure {
                _actionState.value = HandshakeActionState(error = it.message)
            }
        }
    }

    fun clearActionState() {
        _actionState.value = HandshakeActionState()
    }
}
