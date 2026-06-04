import { useCallback } from 'react'
import { Check, Terminal } from 'lucide-react'
import { Button } from '@/shared/ui/button'
import { useCopyToClipboard } from '@/shared/lib/clipboard'

interface InstallButtonProps {
  namespace: string
  name: string
  version?: string
}

/**
 * One-click "Install" button that copies the install command to the clipboard.
 *
 * Copies `skillhub install {namespace}/{name}` (optionally with version pin) and
 * shows "Copied!" feedback for 2 seconds.
 */
export function InstallButton({ namespace, name, version }: InstallButtonProps) {
  const [copied, copy] = useCopyToClipboard(2000)

  const installCommand = version
    ? `skillhub install ${namespace}/${name}@${version}`
    : `skillhub install ${namespace}/${name}`

  const handleInstall = useCallback(async () => {
    try {
      await copy(installCommand)
    } catch (err) {
      // Fallback: select text in a temporary input
      const textarea = document.createElement('textarea')
      textarea.value = installCommand
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      try {
        document.execCommand('copy')
      } catch {
        // Final fallback: show the command in a read-only textarea
        const fallbackContainer = document.createElement('div')
        fallbackContainer.style.position = 'fixed'
        fallbackContainer.style.bottom = '16px'
        fallbackContainer.style.left = '16px'
        fallbackContainer.style.right = '16px'
        fallbackContainer.style.maxWidth = '400px'
        fallbackContainer.style.padding = '12px'
        fallbackContainer.style.background = '#fff'
        fallbackContainer.style.border = '1px solid #ccc'
        fallbackContainer.style.borderRadius = '8px'
        fallbackContainer.style.boxShadow = '0 4px 12px rgba(0,0,0,0.15)'
        fallbackContainer.style.zIndex = '9999'
        const message = document.createElement('p')
        message.style.margin = '0 0 8px'
        message.style.fontSize = '12px'
        message.style.color = '#666'
        message.textContent = 'Copy this command manually:'
        const code = document.createElement('code')
        code.style.fontSize = '13px'
        code.style.wordBreak = 'break-all'
        code.textContent = installCommand
        fallbackContainer.appendChild(message)
        fallbackContainer.appendChild(code)
        document.body.appendChild(fallbackContainer)
        setTimeout(() => fallbackContainer.remove(), 8000)
      }
      document.body.removeChild(textarea)
    }
  }, [copy, installCommand])

  return (
    <Button
      variant={copied ? 'outline' : 'default'}
      size="lg"
      onClick={handleInstall}
      className="gap-2 min-w-[140px]"
    >
      {copied ? (
        <>
          <Check className="h-4 w-4" />
          Copied!
        </>
      ) : (
        <>
          <Terminal className="h-4 w-4" />
          Install
        </>
      )}
    </Button>
  )
}
