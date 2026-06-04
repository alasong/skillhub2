import { useMutation } from '@tanstack/react-query'

export function useInitiateAccountMerge() {
  return useMutation({
    mutationFn: async (_params: { secondaryIdentifier: string }) => ({ mergeRequestId: 0, verificationToken: '', secondaryUserId: '' }),
  })
}

export function useVerifyAccountMerge() {
  return useMutation({
    mutationFn: async (_params: { mergeRequestId: number; verificationToken: string }) => ({}),
  })
}

export function useConfirmAccountMerge() {
  return useMutation({
    mutationFn: async (_params: { mergeRequestId: number }) => ({}),
  })
}
